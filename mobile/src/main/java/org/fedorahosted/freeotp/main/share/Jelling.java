/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp.main.share;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;

import org.fedorahosted.freeotp.R;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class Jelling extends Discoverable {
    private class GattCallback extends BluetoothGattCallback {
        Shareable.ShareCallback mShareCallback;
        boolean mRegistered = false;
        boolean mSuccess = false;
        boolean mRestart = false;
        String mToken;

        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent i) {
                BluetoothDevice dev = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int ns = i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int os = i.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                Log.d("LOG", String.format("Bond: %s (%d => %d)", dev.getAddress(), os, ns));

                if (ns != BluetoothDevice.BOND_BONDED)
                    return;

                if (mBluetoothGatt == null)
                    return;

                if (!dev.equals(mBluetoothGatt.getDevice()))
                    return;

                mRestart = true;
                mBluetoothGatt.disconnect();
            }
        };

        GattCallback(String token, Shareable.ShareCallback shareCallback) {
            mShareCallback = shareCallback;
            mToken = token;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            switch (state) {
                case BluetoothGatt.STATE_CONNECTED:
                    if (!mRegistered) {
                        IntentFilter f = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        mContext.registerReceiver(mBroadcastReceiver, f);
                        mRegistered = true;
                    }
                    gatt.discoverServices();
                    break;

                case BluetoothGatt.STATE_DISCONNECTED:
                    if (mRestart) {
                        // The remote pairing dialog has stolen focus from the input.
                        // Give time for the dialog to dismiss and refocus.
                        post(new Runnable() {
                            @Override
                            public void run() {
                                mBluetoothGatt.connect();
                            }
                        }, 3000);
                        mRestart = false;
                        return;
                    }

                    post(new Runnable() {
                        @Override
                        public void run() {
                            mShareCallback.onShareCompleted(mSuccess);
                        }
                    });

                    if (mRegistered) {
                        mContext.unregisterReceiver(mBroadcastReceiver);
                        mRegistered = false;
                    }

                    mBluetoothGatt = null;
                    gatt.close();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService svc = gatt.getService(JELLING_SVC);
            if (svc == null) {
                Log.d(getClass().getSimpleName(), "Service not found!");
                gatt.disconnect();
                return;
            }

            final BluetoothGattCharacteristic chr = svc.getCharacteristic(JELLING_CHR);
            if (chr == null) {
                Log.d(getClass().getSimpleName(), "Characteristic not found!");
                gatt.disconnect();
                return;
            }

            gatt.beginReliableWrite();
            chr.setValue(mToken);
            if (!gatt.writeCharacteristic(chr)) {
                Log.d(getClass().getSimpleName(), "Error during write!");
                gatt.abortReliableWrite();
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic chr, int status) {
            switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                if (gatt.executeReliableWrite())
                    return;

            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
            default:
                Log.d(getClass().getSimpleName(), String.format("Chr. Write failed: %d", status));
                gatt.abortReliableWrite();
                gatt.disconnect();
                break;
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            mSuccess = status == BluetoothGatt.GATT_SUCCESS;
            gatt.disconnect();
        }
    }

    private static final UUID JELLING_SVC = UUID.fromString("B670003C-0079-465C-9BA7-6C0539CCD67F");
    private static final UUID JELLING_CHR = UUID.fromString("F4186B06-D796-4327-AF39-AC22C50BDCA8");

    private static final List<ScanFilter> FILTERS = Collections.singletonList(
        new ScanFilter.Builder().setServiceUuid(
                new ParcelUuid(JELLING_SVC),
                new ParcelUuid(UUID.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"))
        ).build()
    );

    private static final ScanSettings SCAN_SETTINGS = new ScanSettings.Builder()
        .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build();

    private final ScanCallback mScanCallback = new ScanCallback() {
        private final Map<BluetoothDevice, Long> mDevices = new ConcurrentHashMap<>();
        private final long TIMEOUT = 10000;

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results)
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
        }

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            final BluetoothDevice dev = result.getDevice();

            if (!mDevices.containsKey(dev)) {
                Adapter.Item item = new Adapter.Item();
                item.setTitle(mContext.getResources().getString(R.string.share_jelling_send_to));
                item.setSubtitle(dev.getName());
                item.setImage(R.drawable.ic_bluetooth);

                switch (dev.getBondState()) {
                    case BluetoothDevice.BOND_BONDED:
                    case BluetoothDevice.BOND_BONDING:
                        item.setPriority(100);
                        break;
                    default:
                        item.setPriority(101);
                }

                mDeviceItemMap.put(dev, item);
                appear(item, new Shareable() {
                    @Override
                    public void share(String token, ShareCallback shareCallback) {
                        GattCallback gc = new GattCallback(token, shareCallback);
                        mBluetoothGatt = dev.connectGatt(mContext, false, gc);
                    }
                });
            }

            mDevices.put(dev, System.currentTimeMillis());

            post(new Runnable() {
                @Override
                public void run() {
                    for (BluetoothDevice d : mDevices.keySet()) {
                        if (mDevices.get(d) < System.currentTimeMillis() - TIMEOUT) {
                            disappear(mDeviceItemMap.get(d));
                            mDevices.remove(d);
                        }
                    }
                }
            }, TIMEOUT);
        }
    };

    private Map<BluetoothDevice, Adapter.Item> mDeviceItemMap = new ConcurrentHashMap<>();
    private Adapter.Item mBluetoothItem = new Adapter.Item();
    private BluetoothGatt mBluetoothGatt;
    private boolean mScanning = false;

    Jelling(@NonNull Context context, @NonNull DiscoveryCallback discoveryCallback) {
        super(context, discoveryCallback);

        mBluetoothItem = new Adapter.Item();
        mBluetoothItem.setSubtitle(mContext.getResources().getString(R.string.share_jelling_bluetooth_devices));
        mBluetoothItem.setTitle(mContext.getResources().getString(R.string.share_jelling_scan_for));
        mBluetoothItem.setImage(R.drawable.ic_bluetooth);
        mBluetoothItem.setPriority(102);
        if (supported())
            appear(mBluetoothItem, null);
    }

    @Override
    public boolean supported() {
        PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @Override
    public String[] permissions() {
        return new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
        };
    }

    public Intent enablement() {
        BluetoothManager bm = mContext.getSystemService(BluetoothManager.class);
        if (bm != null) {
            BluetoothAdapter ba = bm.getAdapter();
            if (ba != null && ba.isEnabled())
                return null;
        }

        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }

    @Override
    public void startDiscovery() {
        if (mScanning)
            return;

        BluetoothManager bm = mContext.getSystemService(BluetoothManager.class);
        if (bm == null)
            return;

        BluetoothAdapter ba = bm.getAdapter();
        if (ba == null)
            return;

        ba.getBluetoothLeScanner().startScan(FILTERS, SCAN_SETTINGS, mScanCallback);
        mScanning = true;

        post(new Runnable() {
            @Override
            public void run() {
                mBluetoothItem.setTitle(mContext.getResources().getString(R.string.share_jelling_scanning_for));
                mBluetoothItem.setOnClickListener(null);
            }
        });
    }

    public void stopDiscovery() {
        if (!mScanning)
            return;

        BluetoothManager bm = mContext.getSystemService(BluetoothManager.class);
        if (bm == null)
            return;

        BluetoothAdapter ba = bm.getAdapter();
        if (ba == null)
            return;

        ba.getBluetoothLeScanner().stopScan(mScanCallback);
        mScanning = false;

        if (mBluetoothGatt != null)
            mBluetoothGatt.disconnect();

        mBluetoothItem.setTitle(mContext.getResources().getString(R.string.share_jelling_scan_for));
        disappear(mBluetoothItem);
        appear(mBluetoothItem, null);
    }

    @Override
    boolean isDiscovering() {
        return mScanning;
    }
}

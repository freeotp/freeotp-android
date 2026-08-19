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
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.os.Build;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import android.widget.Toast;

import org.fedorahosted.freeotp.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressLint("DiscouragedPrivateApi")
class Jelling extends Discoverable {

    private interface GattProbeCallback {
        void probeResult(BluetoothDevice dev, boolean supported);
        void shareCallback(BluetoothDevice dev, boolean success);
    }

    private class GattCallback extends BluetoothGattCallback {
        private final GattProbeCallback mCallback;
        private boolean mSuccess = false;
        private BluetoothGattCharacteristic mChr = null;
        private BluetoothGatt mGatt = null;

        GattCallback(GattProbeCallback cb) {
            mCallback = cb;
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            super.onConnectionStateChange(gatt, status, state);
            switch (state) {
            case BluetoothGatt.STATE_CONNECTED:
                post(gatt::discoverServices, 300);
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                post(() -> mCallback.shareCallback(gatt.getDevice(), mSuccess));
                gatt.close();
                if (gatt.equals(mGatt))
                    mGatt = null;
                break;
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService mSvc = gatt.getService(JELLING_SVC);
            if (mSvc == null) {
                refreshCache(gatt);
                gatt.disconnect();
                mCallback.probeResult(gatt.getDevice(), false);
                return;
            }

            mChr = mSvc.getCharacteristic(JELLING_CHR);
            if (mChr == null) {
                refreshCache(gatt);
                gatt.disconnect();
                mCallback.probeResult(gatt.getDevice(), false);
                return;
            }

            mCallback.probeResult(gatt.getDevice(), true);
            mGatt = gatt;
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        void sendToken(String token) {
            if (mGatt == null || mChr == null) return;

            mGatt.beginReliableWrite();
            mChr.setValue(token.getBytes(StandardCharsets.UTF_8));
            if(! mGatt.writeCharacteristic(mChr)) {
                mGatt.abortReliableWrite();
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                if (gatt.executeReliableWrite())
                    return;
            if (status == 0x1 || status == 133 || status == BluetoothGatt.GATT_FAILURE) {
                post(() -> Toast.makeText(mContext, R.string.share_jelling_stale_handles, Toast.LENGTH_LONG).show());
                refreshCache(gatt);
            }
            gatt.disconnect();
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            mSuccess = status == BluetoothGatt.GATT_SUCCESS;
            gatt.disconnect();
        }
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        protected void disconnect() {
            if (mGatt == null) return;
            mGatt.disconnect();
        }
    }
    private static final UUID JELLING_SVC = UUID.fromString("B670003C-0079-465C-9BA7-6C0539CCD67F");
    private static final UUID JELLING_CHR = UUID.fromString("F4186B06-D796-4327-AF39-AC22C50BDCA8");

    private static final String[] mPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ? new String[] {
            Manifest.permission.BLUETOOTH_CONNECT,
        }
        : new String[] {
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
        };

    /**
     * <p>Android caches Gatt services and handles, so
     * if BlueZ/Jelling restarts, all handles becomes
     * invalid, causing the write to fail.</p>
     * <br />
     * <p>Resolve BluetoothGatt.refresh() via reflection.</p>
     */
    private static void refreshCache(BluetoothGatt gatt) {
        if(mGattRefresh == null || gatt == null) return;
        try {
            mGattRefresh.invoke(gatt);
        } catch (IllegalAccessException | InvocationTargetException ignored) {}
    }

    private final Map<String, Adapter.Item> mDeviceItemMap = new ConcurrentHashMap<>();
    private final Map<String, GattCallback> mGattCallbacks = new ConcurrentHashMap<>();
    private final Map<String, Shareable.ShareCallback> mShareCallbacks = new ConcurrentHashMap<>();
    private final Adapter.Item mBluetoothItem = new Adapter.Item();
    private boolean mScanning = false;
    private int remoteFound = -1;
    private final GattProbeCallback probeCallback = new GattProbeCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void probeResult(BluetoothDevice dev, boolean supported) {
            String address = dev.getAddress();
            if(remoteFound > 0)
                remoteFound -= 1;
            else
                disappear(mBluetoothItem);

            if (! supported || mDeviceItemMap.containsKey(address)) {
                mGattCallbacks.remove(address);
                return;
            }

            Adapter.Item item = new Adapter.Item();
            item.setTitle(mContext.getResources().getString(R.string.share_jelling_send_to));
            item.setSubtitle(getAliasOrName(dev));
            item.setImage(R.drawable.ic_bluetooth);
            item.setPriority(100);

            mDeviceItemMap.put(address, item);
            appear(item, (token, shareCallback) -> {
                mShareCallbacks.put(address, shareCallback);
                Objects.requireNonNull(mGattCallbacks.get(address)).sendToken(token);
            });
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void shareCallback(BluetoothDevice dev, boolean success) {
            String address = dev.getAddress();
            if (! mShareCallbacks.containsKey(address)) return;
            Objects.requireNonNull(mShareCallbacks.get(address)).onShareCompleted(success);
            disconnect();
        }
    };

    private static final Method mGattRefresh;
    static {
        Method m = null;
        try {
            //noinspection JavaReflectionMemberAccess
            m = BluetoothGatt.class.getDeclaredMethod("refresh");
            m.setAccessible(true);
        } catch (NoSuchMethodException ignored) {}
        mGattRefresh = m;
    }


    Jelling(@NonNull Context context, @NonNull DiscoveryCallback discoveryCallback) {
        super(context, discoveryCallback);

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
        return mPermissions;
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    void filterDevices(Set<BluetoothDevice> devices) {
        for(BluetoothDevice dev : devices) {
            String address = dev.getAddress();
            BluetoothClass bc = dev.getBluetoothClass();
            switch (bc.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                // Windows sometimes reports itself as uncategorized when jelling is active
                case BluetoothClass.Device.Major.UNCATEGORIZED:
                    break;
                default:
                    continue;
            }
            if (mGattCallbacks.containsKey(address)) continue;
            remoteFound += 1;

            GattCallback gc = new GattCallback(probeCallback);
            mGattCallbacks.put(address, gc);
            dev.connectGatt(mContext, false, gc);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
        mScanning = true;
        post(() -> {
            mBluetoothItem.setTitle(mContext.getResources().getString(R.string.share_jelling_scanning_for));
            mBluetoothItem.setOnClickListener(null);
        });
        post(() -> filterDevices(ba.getBondedDevices()));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void stopDiscovery() {
        if (!mScanning)
            return;

        disconnect();
        mScanning = false;
        remoteFound = -1;

        mBluetoothItem.setTitle(mContext.getResources().getString(R.string.share_jelling_scan_for));
        disappear(mBluetoothItem);
        appear(mBluetoothItem, null);
    }

    @Override
    boolean isDiscovering() {
        return mScanning;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @NonNull
    private static String getAliasOrName(@NonNull BluetoothDevice dev) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            if(dev.getAlias() != null)
                return  dev.getAlias();
        return  dev.getName();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void disconnect() {
        for(String dev: mGattCallbacks.keySet()) {
            mShareCallbacks.remove(dev);

            if(mDeviceItemMap.containsKey(dev)) {
                disappear(mDeviceItemMap.get(dev));
                mDeviceItemMap.remove(dev);
            }

            GattCallback cb = mGattCallbacks.get(dev);
            if (cb != null) {
                cb.disconnect();
            }
            mGattCallbacks.remove(dev);
        }
    }
}

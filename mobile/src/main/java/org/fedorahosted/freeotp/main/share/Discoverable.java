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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

class Discoverable {
    interface Shareable {
        interface ShareCallback {
            void onShareCompleted(boolean success);
        }

        void share(String token, ShareCallback shareCallback);
    }

    interface DiscoveryCallback {
        void onShareAppeared(Discoverable discoverable, Adapter.Item item, Shareable shareable);
        void onShareDisappeared(Discoverable discoverable, Adapter.Item item);
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private DiscoveryCallback mDiscoveryCallback;
    Context mContext;

    Discoverable(@NonNull Context context, @NonNull DiscoveryCallback discoveryCallback) {
        mDiscoveryCallback = discoveryCallback;
        mContext = context;
    }

    void post(Runnable runnable) {
        mHandler.post(runnable);
    }

    void post(Runnable runnable, long delayMillis) {
        mHandler.postDelayed(runnable, delayMillis);
    }

    void appear(final Adapter.Item item, final Shareable shareable) {
        post(new Runnable() {
            @Override
            public void run() {
                mDiscoveryCallback.onShareAppeared(Discoverable.this, item, shareable);
            }
        });
    }

    void disappear(final Adapter.Item item) {
        post(new Runnable() {
            @Override
            public void run() {
                mDiscoveryCallback.onShareDisappeared(Discoverable.this, item);
            }
        });
    }

    /* Determines whether or not this device supports this type of share. */
    boolean supported() {
        return true;
    }

    /* The permissions required to use this type of share. */
    String[] permissions() {
        return new String[0];
    }

    boolean permitted() {
        if (!supported())
            return false;

        for (String p : permissions()) {
            if (mContext.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    /* The intent to use with startActivityForResult() if enabled is required. */
    Intent enablement() {
        return null;
    }

    /* Start discovery of sharables. */
    void startDiscovery() {}

    /* Stop discovery of sharables. */
    void stopDiscovery() {}

    boolean isDiscovering() { return true; }
}

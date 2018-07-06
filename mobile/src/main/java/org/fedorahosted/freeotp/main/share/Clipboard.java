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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.NonNull;

import org.fedorahosted.freeotp.R;

class Clipboard extends Discoverable {
    private ClipboardManager mClipboardManager;

    Clipboard(@NonNull Context context, @NonNull DiscoveryCallback discoveryCallback) {
        super(context, discoveryCallback);

        mClipboardManager = context.getSystemService(ClipboardManager.class);
        if (mClipboardManager == null)
            return;

        Adapter.Item item = new Adapter.Item();
        item.setImage(R.drawable.ic_copy);
        item.setTitle(mContext.getResources().getString(R.string.share_clipboard_copy_to));
        item.setSubtitle(mContext.getResources().getString(R.string.share_clipboard_clipboard));

        appear(item, new Shareable() {
            @Override
            public void share(String token, ShareCallback shareCallback) {
                mClipboardManager.setPrimaryClip(ClipData.newPlainText(null, token));
                shareCallback.onShareCompleted(true);
            }
        });
    }
}

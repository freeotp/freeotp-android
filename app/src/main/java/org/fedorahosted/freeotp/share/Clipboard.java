package org.fedorahosted.freeotp.share;

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
        item.setImage(R.drawable.copy);
        item.setTitle(mContext.getResources().getString(R.string.copy_to));
        item.setSubtitle(mContext.getResources().getString(R.string.clipboard));

        appear(item, new Shareable() {
            @Override
            public void share(String token, ShareCallback shareCallback) {
                mClipboardManager.setPrimaryClip(ClipData.newPlainText(null, token));
                shareCallback.onShareCompleted(true);
            }
        });
    }
}

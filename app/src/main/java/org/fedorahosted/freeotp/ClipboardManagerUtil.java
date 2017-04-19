package org.fedorahosted.freeotp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by root on 18/04/17.
 */
public class ClipboardManagerUtil {
    private ClipboardManagerUtil() {
        // no-op
    }

    /**
     * Copies a {@link String} to the clipboard and shows a {@link Toast} informing the user that the code was copied.
     */
    public static void copyToClipboard(Context context, ClipboardManager manager, String code) {
        manager.setPrimaryClip(ClipData.newPlainText(null, code));
        Toast.makeText(context,
                R.string.code_copied,
                Toast.LENGTH_SHORT).show();
    }
}

package org.fedorahosted.freeotp.utils;

import android.app.Activity;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

public class UserNotifier {
    public static void show(Activity activity, String msg) {
        Snackbar.make(activity.findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }

    public static void show(Activity activity, @StringRes int msgId) {
        show(activity, activity.getString(msgId));
    }

    public static void show(Activity activity, @StringRes int msgId, @StringRes int placeholderMsgId) {
        show(activity, activity.getString(msgId, activity.getString(placeholderMsgId)));
    }
}

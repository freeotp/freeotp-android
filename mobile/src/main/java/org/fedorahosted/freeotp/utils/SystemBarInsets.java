package org.fedorahosted.freeotp.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;

/**
 * Android 16 ignores windowOptOutEdgeToEdgeEnforcement for apps that target API 36, so the
 * activity is drawn behind the status and navigation bars. Keep the content out of the bars:
 * the app bar is padded by the top inset (its color then fills the status bar area) and the
 * layout root by the other insets, including the keyboard.
 *
 * When the window is not edge-to-edge (older Android, or API 35 with the opt-out), the insets
 * that reach the layout are zero and nothing changes.
 */
public final class SystemBarInsets {
    private SystemBarInsets() {
    }

    public static void apply(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0)
            return;

        View root = content.getChildAt(0);
        View appBar = null;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount() && appBar == null; i++) {
                if (group.getChildAt(i) instanceof AppBarLayout)
                    appBar = group.getChildAt(i);
            }
        }
        apply(root, appBar);
    }

    private static void apply(View root, View appBar) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        final int appBarTop = appBar == null ? 0 : appBar.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int barsBottom = Math.max(bars.bottom, ime.bottom);

            if (appBar == null) {
                v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + barsBottom);
            } else {
                v.setPadding(left + bars.left, top, right + bars.right, bottom + barsBottom);
                appBar.setPadding(appBar.getPaddingLeft(), appBarTop + bars.top,
                        appBar.getPaddingRight(), appBar.getPaddingBottom());
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }
}

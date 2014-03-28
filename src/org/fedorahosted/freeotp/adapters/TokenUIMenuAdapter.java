package org.fedorahosted.freeotp.adapters;

import java.util.regex.Pattern;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class TokenUIMenuAdapter extends TokenUIClickAdapter {
    private static final Pattern PATTERN = Pattern.compile("^\\d+$");
    private static final String TITLE = "FreeOTP Token Code";
    private final ClipboardManager mClipboardManager;

    public TokenUIMenuAdapter(Activity activity) {
        super(activity);
        mClipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    protected void bindView(View view, final int position, Token token) {
        super.bindView(view, position, token);

        final TextView code = (TextView) view.findViewById(R.id.code);

        view.findViewById(R.id.menuButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pm = new PopupMenu(v.getContext(), v);
                pm.getMenuInflater().inflate(R.menu.token, pm.getMenu());

                // Verify we can copy the text.
                boolean enabled = PATTERN.matcher(code.getText()).matches();
                pm.getMenu().findItem(R.id.action_copy).setEnabled(enabled);

                pm.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.action_copy:
                            if (PATTERN.matcher(code.getText()).matches()) {
                                ClipData cd = ClipData.newPlainText(TITLE, code.getText());
                                mClipboardManager.setPrimaryClip(cd);
                            }
                            break;

                        case R.id.action_delete:
                            new TokenPersistence(code.getContext()).delete(position);
                            notifyDataSetChanged();
                            break;
                        }

                        return true;
                    }
                });

                pm.show();
            }
        });
    }
}

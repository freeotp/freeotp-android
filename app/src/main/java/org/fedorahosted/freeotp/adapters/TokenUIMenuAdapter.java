package org.fedorahosted.freeotp.adapters;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.dialogs.DeleteDialogActivity;
import org.fedorahosted.freeotp.dialogs.RenameDialogActivity;

import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class TokenUIMenuAdapter extends TokenUIClickAdapter {
    public TokenUIMenuAdapter(Activity activity) {
        super(activity);
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

                pm.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.action_rename:
                            RenameDialogActivity.startActivity(code.getContext(), position,
                                                               RenameDialogActivity.class);
                            break;

                        case R.id.action_delete:
                            DeleteDialogActivity.startActivity(code.getContext(), position,
                                                               DeleteDialogActivity.class);
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

package org.fedorahosted.freeotp.dialogs;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class DeleteDialogActivity extends BasePositionedDialogActivity {
    public DeleteDialogActivity() {
        super(R.layout.delete, android.R.string.cancel, 0, R.string.delete);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Token token = new TokenPersistence(this).get(getPosition());
        ((TextView) findViewById(R.id.issuer)).setText(token.getIssuer());
        ((TextView) findViewById(R.id.label)).setText(token.getLabel());
    }

    @Override
    protected void onClick(View v, int which) {
        if (which != BUTTON_POSITIVE)
            return;

        new TokenPersistence(this).delete(getPosition());
    }
}

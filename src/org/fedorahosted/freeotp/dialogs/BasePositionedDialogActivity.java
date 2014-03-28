package org.fedorahosted.freeotp.dialogs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public abstract class BasePositionedDialogActivity extends BaseDialogActivity {
    private static final String POSITION = "position";
    private int                 mPosition;

    public BasePositionedDialogActivity(int layout, int negative, int neutral, int positive) {
        super(layout, negative, neutral, positive);
    }

    public static void startActivity(Context ctx, int position,
            Class<? extends BasePositionedDialogActivity> cls) {
        Intent i = new Intent(ctx, cls);
        i.putExtra(POSITION, position);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the position we wish to rename. This MUST exist.
        mPosition = getIntent().getIntExtra(POSITION, -1);
        if (mPosition < 0)
            finish();
    }

    protected int getPosition() {
        return mPosition;
    }
}

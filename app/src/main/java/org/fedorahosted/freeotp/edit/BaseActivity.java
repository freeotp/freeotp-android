package org.fedorahosted.freeotp.edit;

import android.app.Activity;
import android.os.Bundle;

import org.fedorahosted.freeotp.GPSActivity;

public abstract class BaseActivity extends GPSActivity {
    public static final String EXTRA_POSITION = "position";
    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the position of the token. This MUST exist.
        mPosition = getIntent().getIntExtra(EXTRA_POSITION, -1);
        assert mPosition >= 0;
    }

    protected int getPosition() {
        return mPosition;
    }
}

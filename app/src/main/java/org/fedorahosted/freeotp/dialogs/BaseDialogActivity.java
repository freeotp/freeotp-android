/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2014  Nathaniel McCallum, Red Hat
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

package org.fedorahosted.freeotp.dialogs;

import org.fedorahosted.freeotp.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public abstract class BaseDialogActivity extends Activity {
    public static final int BUTTON_NEGATIVE = 0;
    public static final int BUTTON_NEUTRAL = 1;
    public static final int BUTTON_POSITIVE = 2;

    private final int mLayout;
    private final int mNegative;
    private final int mNeutral;
    private final int mPositive;

    public BaseDialogActivity(int layout, int negative, int neutral, int positive) {
        super();
        mLayout = layout;
        mNegative = negative;
        mNeutral = neutral;
        mPositive = positive;
    }

    private void makeButton(int id, int text, final int which) {
        Button b = (Button) findViewById(id);

        if (text == 0) {
            b.setVisibility(View.GONE);
            return;
        }

        b.setText(text);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseDialogActivity.this.onClick(v, which);
                finish();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        // Inflate the provided content layout.
        View view = View.inflate(this, mLayout, null);
        view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                              LayoutParams.WRAP_CONTENT, 1f));

        // Add it to the content area.
        ((LinearLayout) findViewById(R.id.dialog)).addView(view, 0);

        // Enable/disable specified buttons.
        makeButton(R.id.dialog_button_negative, mNegative, BUTTON_NEGATIVE);
        if (mNegative == 0)
            findViewById(R.id.dialog_button_divider_negative).setVisibility(View.GONE);

        makeButton(R.id.dialog_button_neutral, mNeutral, BUTTON_NEUTRAL);
        if (mNeutral == 0)
            findViewById(R.id.dialog_button_divider_neutral).setVisibility(View.GONE);

        makeButton(R.id.dialog_button_positive, mPositive, BUTTON_POSITIVE);
    }

    protected Button getButton(int which) {
        switch (which) {
        case BUTTON_NEGATIVE:
            return (Button) findViewById(R.id.dialog_button_negative);
        case BUTTON_NEUTRAL:
            return (Button) findViewById(R.id.dialog_button_neutral);
        case BUTTON_POSITIVE:
            return (Button) findViewById(R.id.dialog_button_positive);
        }

        return null;
    }

    protected abstract void onClick(View v, int which);
}

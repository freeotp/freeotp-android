/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
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

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

public class ManualTextWatcher implements TextWatcher {
    private final Button   mButton;
    private final EditText mIssuer;
    private final EditText mLabel;
    private final EditText mSecret;
    private final EditText mInterval;

    public ManualTextWatcher(BaseDialogActivity bda) {
        mButton = bda.getButton(BaseDialogActivity.BUTTON_POSITIVE);
        mIssuer = (EditText) bda.findViewById(R.id.issuer);
        mLabel = (EditText) bda.findViewById(R.id.label);
        mSecret = (EditText) bda.findViewById(R.id.secret);
        mInterval = (EditText) bda.findViewById(R.id.interval);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mButton.setEnabled(false);

        if (mIssuer.getText().length() == 0)
            return;

        if (mLabel.getText().length() == 0)
            return;

        if (mSecret.getText().length() == 0 || mSecret.getText().length() % 8 != 0)
            return;

        if (mInterval.getText().length() == 0)
            return;

        mButton.setEnabled(true);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}

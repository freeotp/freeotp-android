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

package org.fedorahosted.freeotp;

import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

public class AddTokenTextWatcher implements TextWatcher {
	private final AlertDialog dialog;

	public AddTokenTextWatcher(AlertDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

		b.setEnabled(false);

		if (((EditText) dialog.findViewById(R.id.issuer)).getText().length() == 0)
			return;

		if (((EditText) dialog.findViewById(R.id.id)).getText().length() == 0)
			return;

		if (((EditText) dialog.findViewById(R.id.secret)).getText().length() == 0 ||
			((EditText) dialog.findViewById(R.id.secret)).getText().length() % 8 != 0)
			return;

		if (((EditText) dialog.findViewById(R.id.interval)).getText().length() == 0)
			return;

		b.setEnabled(true);
	}

	@Override
	public void afterTextChanged(Editable s) {

	}
}

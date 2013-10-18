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

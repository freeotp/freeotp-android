package org.fedorahosted.freeotp;

import android.app.AlertDialog;
import android.text.Editable;

public class AddTokenSecretTextWatcher extends AddTokenTextWatcher {
	public AddTokenSecretTextWatcher(AlertDialog dialog) {
		super(dialog);
	}

	@Override
	public void afterTextChanged(Editable s) {
		super.afterTextChanged(s);

		if (s.length() == 0)
			return;

		boolean haveData = false;
		for (int i = s.length() - 1; i >= 0; i--) {
			char c = s.charAt(i);
			if (c != '=')
				haveData = true;
			else if (haveData)
				s.delete(i, i + 1);
		}
	}
}

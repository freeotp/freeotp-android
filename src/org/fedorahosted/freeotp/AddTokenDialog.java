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

import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public abstract class AddTokenDialog extends AlertDialog {
	private final int SHA1_OFFSET = 1;
	private final int TOTP_OFFSET = 0;

	public AddTokenDialog(Context ctx) {
		super(ctx);

		setTitle(R.string.add_token);
		setView(getLayoutInflater().inflate(R.layout.manual, null));

		setButton(BUTTON_NEGATIVE, ctx.getString(android.R.string.cancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		setButton(BUTTON_POSITIVE, ctx.getString(R.string.add), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Get the fields
				String issuer = Uri.encode(((EditText) findViewById(R.id.issuer)).getText().toString());
				String id = Uri.encode(((EditText) findViewById(R.id.id)).getText().toString());
				String secret = Uri.encode(((EditText) findViewById(R.id.secret)).getText().toString());
				String type = ((Spinner) findViewById(R.id.type)).getSelectedItemId() == TOTP_OFFSET ? "totp" : "hotp";
				String algorithm = ((Spinner) findViewById(R.id.algorithm)).getSelectedItem().toString().toLowerCase(Locale.US);
				int interval = Integer.parseInt(((EditText) findViewById(R.id.interval)).getText().toString());
				int digits = ((RadioButton) findViewById(R.id.digits6)).isChecked() ? 6 : 8;

				// Create the URI
				String uri = String.format(Locale.US, "otpauth://%s/%s:%s?secret=%s&algorithm=%s&digits=%d",
                                           type, issuer, id, secret, algorithm, digits);
				if (type.equals("totp"))
					uri = uri.concat(String.format("&period=%d", interval));
				else
					uri = uri.concat(String.format("&counter=%d", interval));

				// Add the token
				addToken(uri);
			}
		});
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();

		// Disable the Add button
		getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

		// Set constraints on when the Add button is enabled
		((EditText) findViewById(R.id.issuer)).addTextChangedListener(new AddTokenTextWatcher(this));
		((EditText) findViewById(R.id.id)).addTextChangedListener(new AddTokenTextWatcher(this));
		((EditText) findViewById(R.id.secret)).addTextChangedListener(new AddTokenSecretTextWatcher(this));
		((EditText) findViewById(R.id.interval)).addTextChangedListener(new AddTokenTextWatcher(this));

		// Select the default algorithm
		((Spinner) findViewById(R.id.algorithm)).setSelection(SHA1_OFFSET);

		// Setup the Interval / Counter toggle
		((Spinner) findViewById(R.id.type)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					((TextView) findViewById(R.id.interval_label)).setText(R.string.interval);
					((EditText) findViewById(R.id.interval)).setText("30");
				} else {
					((TextView) findViewById(R.id.interval_label)).setText(R.string.counter);
					((EditText) findViewById(R.id.interval)).setText("0");
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

	}

	public abstract void addToken(String uri);
}

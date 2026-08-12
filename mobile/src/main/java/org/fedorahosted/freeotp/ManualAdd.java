package org.fedorahosted.freeotp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import org.fedorahosted.freeotp.databinding.ActivityManualAddBinding;
import org.fedorahosted.freeotp.main.Activity;
import org.fedorahosted.freeotp.utils.UserNotifier;

public class ManualAdd extends AppCompatActivity {
    ActivityManualAddBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityManualAddBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        /* Toolbar */
        setSupportActionBar(mBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* Setup the algorithm spinner */
        ArrayAdapter<CharSequence> algorithmAdapter = ArrayAdapter.createFromResource(this, R.array.algorithms_array, android.R.layout.simple_spinner_item);
        algorithmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBinding.algorithmSpinner.setAdapter(algorithmAdapter);
        mBinding.algorithmSpinner.setSelection(algorithmAdapter.getPosition("SHA256"));

        /* Setup the intervals spinner */
        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(this, R.array.intervals_array, android.R.layout.simple_spinner_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBinding.intervalSpinner.setAdapter(intervalAdapter);
        mBinding.intervalSpinner.setSelection(intervalAdapter.getPosition("30"));

        mBinding.add.setOnClickListener(v -> {
            if (!inputValid()) {
                return;
            }

            Intent intent = new Intent();
            intent.setData(makeUri());
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }

    private Uri makeUri() {
        String label = String.format("%s:%s", mBinding.issuer.getText(), mBinding.account.getText());
        // Cause i18n，this text may not TOTP OR HOTP
        String type = "";
        final int id = mBinding.type.getCheckedRadioButtonId();
        if (id == R.id.button_totp) {
            type = "totp";
        } else if (id == R.id.button_hotp) {
            type = "hotp";
        }

        // Validate URI first or Activity will crash
        final TextView algorithm = (TextView)mBinding.algorithmSpinner.getSelectedView();
        final TextView digits = findViewById(mBinding.digits.getCheckedRadioButtonId());
        final TextView interval = (TextView)mBinding.intervalSpinner.getSelectedView();
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("otpauth")
                .authority(type)
                .appendPath(label)
                .appendQueryParameter("secret", mBinding.secret.getText().toString())
                .appendQueryParameter("algorithm", algorithm.getText().toString())
                .appendQueryParameter("digits", digits.getText().toString())
                .appendQueryParameter("period", interval.getText().toString());

        if (type.equals("hotp")) {
            builder.appendQueryParameter("counter", "0");
        }
        return builder.build();
    }

    private boolean inputValid() {
        String secret = mBinding.secret.getText().toString();
        String issuer = mBinding.issuer.getText().toString();
        String account = mBinding.account.getText().toString();
        @StringRes int msgId = 0;

        if (TextUtils.isEmpty(secret)) {
            msgId = R.string.manual_empty_secret;
        } else if (issuer.contains(":") || account.contains(":")) {
            msgId = R.string.manual_malformed_issuer_account;
        }

        if (msgId != 0) {
            UserNotifier.show(this, msgId);
            return false;
        } else {
            return true;
        }
    }
}
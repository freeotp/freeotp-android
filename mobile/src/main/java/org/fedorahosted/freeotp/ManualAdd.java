package org.fedorahosted.freeotp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.codec.binary.StringUtils;
import org.fedorahosted.freeotp.main.Activity;

public class ManualAdd extends AppCompatActivity {
    EditText mAccount;
    EditText mIssuer;
    EditText mSecret;
    RadioGroup mTypeGroup;
    RadioButton mType;
    RadioGroup mDigitsGroup;
    RadioButton mDigits;
    Spinner mAlgorithmSpinner;
    TextView mAlgorithm;
    Spinner mIntervalSpinner;
    TextView mInterval;

    /* Initialize UI views */
    private void initViews() {

        /* Toolbar */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* Setup the algorithm spinner */
        mAlgorithmSpinner = (Spinner) findViewById(R.id.spinner_algorithms);
        ArrayAdapter<CharSequence> algo_adapter = ArrayAdapter.createFromResource(this, R.array.algorithms_array, android.R.layout.simple_spinner_item);
        algo_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAlgorithmSpinner.setAdapter(algo_adapter);
        int selectedDefault = algo_adapter.getPosition("SHA256");
        mAlgorithmSpinner.setSelection(selectedDefault);

        /* Setup the intervals spinner */
        mIntervalSpinner = (Spinner) findViewById(R.id.spinner_intervals);
        ArrayAdapter<CharSequence> interval_adapter = ArrayAdapter.createFromResource(this, R.array.intervals_array, android.R.layout.simple_spinner_item);
        interval_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mIntervalSpinner.setAdapter(interval_adapter);
        selectedDefault = interval_adapter.getPosition("30");
        mIntervalSpinner.setSelection(selectedDefault);

        mAccount = (EditText) findViewById(R.id.edit_text_account);
        mIssuer = (EditText) findViewById(R.id.edit_text_issuer);
        mSecret = (EditText) findViewById(R.id.edit_text_secret);
        mTypeGroup = (RadioGroup) findViewById(R.id.radio_grp_type);
        mDigitsGroup = (RadioGroup) findViewById(R.id.radio_grp_digits);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_add);


        initViews();
    }

    private void getSelected() {
        /* Get Selected radio button from both radio groups */
        int selectedId = mTypeGroup.getCheckedRadioButtonId();
        mType = (RadioButton) findViewById(selectedId);
        selectedId = mDigitsGroup.getCheckedRadioButtonId();
        mDigits = (RadioButton) findViewById(selectedId);

        /* Get selected spinner items */
        mAlgorithm = (TextView)mAlgorithmSpinner.getSelectedView();
        mInterval = (TextView)mIntervalSpinner.getSelectedView();
    }

    private Uri makeUri() {
        String label = String.format("%s:%s", mIssuer.getText(), mAccount.getText());
        String type = mType.getText().toString().toLowerCase();

        // Validate URI first or Activity will crash
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("otpauth")
                .authority(type)
                .appendPath(label)
                .appendQueryParameter("secret", mSecret.getText().toString())
                .appendQueryParameter("algorithm", mAlgorithm.getText().toString())
                .appendQueryParameter("digits", mDigits.getText().toString())
                .appendQueryParameter("period", mInterval.getText().toString());

        if (type.equals("hotp")) {
            builder.appendQueryParameter("counter", "0");
        }
        return builder.build();
    }

    private boolean inputValid() {
        String secret = mSecret.getText().toString();
        String issuer = mIssuer.getText().toString();
        String account = mAccount.getText().toString();
        Boolean valid = true;
        String msg = "";

        if (TextUtils.isEmpty(secret)) {
            msg = "Secret must not be empty";
            valid = false;
        }

        if(issuer.contains(":") || account.contains(":")) {
            msg = "Issuer and account may not contain \":\"";
            valid = false;
        }

        if (!valid) {
            Toast.makeText(getApplicationContext(), msg,Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    public void addToken(View view) {
        if (!inputValid()) {
            return;
        }
        getSelected();
        Uri uri = makeUri();

        Intent intent = new Intent();
        intent.setData(uri);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
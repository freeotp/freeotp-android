package org.fedorahosted.freeotp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import me.gosimple.nbvcxz.Nbvcxz;
import me.gosimple.nbvcxz.resources.Configuration;
import me.gosimple.nbvcxz.resources.ConfigurationBuilder;
import me.gosimple.nbvcxz.scoring.Result;

public class PasswordActivity extends AppCompatActivity {
    private static class Evaluator extends AsyncTask<String, Void, Result> {
        interface OnResultListener {
            void onResult(Result result);
        }

        private OnResultListener mOnResultListener;
        private Locale mLocale;

        Evaluator(Locale locale, OnResultListener listener) {
            mOnResultListener = listener;
            mLocale = locale;
        }

        @Override
        protected Result doInBackground(String... strings) {
            Configuration cfg = new ConfigurationBuilder().setLocale(mLocale).createConfiguration();
            return new Nbvcxz(cfg).estimate(strings[0]);
        }

        @Override
        protected void onPostExecute(Result result) {
            mOnResultListener.onResult(result);
        }
    }

    private AppCompatEditText mPassword;
    private AppCompatEditText mConfirm;
    private MaterialButton mDone;
    private Evaluator mEvaluator;
    private ProgressBar mProgress;
    private TextInputLayout mPasswordLayout;
    private TextInputLayout mConfirmLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        mPassword = findViewById(R.id.password);
        mConfirm = findViewById(R.id.confirm);
        mDone = findViewById(R.id.done);
        mProgress = findViewById(R.id.progress);
        mPasswordLayout = findViewById(R.id.password_layout);
        mConfirmLayout = findViewById(R.id.confirm_layout);

        mDone.setEnabled(true);
        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mConfirmLayout.setVisibility(View.INVISIBLE);
                mConfirmLayout.setError(null);
                mPasswordLayout.setError(null);
                mDone.setEnabled(false);
                mConfirm.setText("");

                if (mEvaluator != null)
                    mEvaluator.cancel(true);

                if (editable.toString().length() == 0) {
                    mPasswordLayout.setPasswordVisibilityToggleEnabled(true);
                    mProgress.setVisibility(View.INVISIBLE);
                    return;
                }

                mEvaluator = new Evaluator(getResources().getConfiguration().locale, new Evaluator.OnResultListener() {
                    @Override
                    public void onResult(Result result) {
                        mPasswordLayout.setPasswordVisibilityToggleEnabled(true);
                        mProgress.setVisibility(View.INVISIBLE);

                        String error = getResources().getString(R.string.password_weak);
                        boolean safe = result.isMinimumEntropyMet();

                        mConfirmLayout.setVisibility(safe ? View.VISIBLE : View.INVISIBLE);
                        mPasswordLayout.setError(safe ? null : error);
                    }
                });

                mPasswordLayout.setPasswordVisibilityToggleEnabled(false);
                mProgress.setVisibility(View.VISIBLE);

                mEvaluator.execute(editable.toString());
            }
        });

        mConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String error = getResources().getString(R.string.password_match);
                String password = mPassword.getText().toString();
                String confirm = editable.toString();
                boolean match = confirm.equals(password);
                boolean zero = confirm.length() == 0;

                mConfirmLayout.setError(zero || match ? null : error);
                mDone.setEnabled(!zero && match);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mEvaluator != null)
            mEvaluator.cancel(true);
    }
}

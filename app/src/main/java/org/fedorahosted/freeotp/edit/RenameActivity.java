package org.fedorahosted.freeotp.edit;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class RenameActivity extends BaseActivity implements TextWatcher {
    private TokenPersistence   mTokenPersistence;
    private EditText           mIssuer;
    private EditText           mLabel;
    private Button             mRestore;
    private Button             mSave;

    private void setupEditTexts(Token token) {
        mLabel.setText(token.getLabel());
        mIssuer.setText(token.getIssuer());
        mIssuer.setSelection(mIssuer.getText().length());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rename);

        mTokenPersistence = new TokenPersistence(this);
        mIssuer = (EditText) findViewById(R.id.issuer);
        mLabel = (EditText) findViewById(R.id.label);
        mRestore = (Button) findViewById(R.id.restore);
        mSave = (Button) findViewById(R.id.save);

        mIssuer.addTextChangedListener(this);
        mLabel.addTextChangedListener(this);

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Token token = mTokenPersistence.get(getPosition());
                token.setIssuer(mIssuer.getText().toString());
                token.setLabel(mLabel.getText().toString());
                mTokenPersistence.save(token);
                finish();
            }
        });

        findViewById(R.id.restore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Token token = mTokenPersistence.get(getPosition());
                token.setIssuer(null);
                token.setLabel(null);
                setupEditTexts(token);
            }
        });

        setupEditTexts(mTokenPersistence.get(getPosition()));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Token token = mTokenPersistence.get(getPosition());
        String label = mLabel.getText().toString();
        String issuer = mIssuer.getText().toString();

        mSave.setEnabled(!label.equals(token.getLabel()) || !issuer.equals(token.getIssuer()));

        token.setIssuer(null);
        token.setLabel(null);

        mRestore.setEnabled(!label.equals(token.getLabel()) || !issuer.equals(token.getIssuer()));
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}

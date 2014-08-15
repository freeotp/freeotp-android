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

package org.fedorahosted.freeotp.edit;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.squareup.picasso.Picasso;

public class EditActivity extends BaseActivity implements TextWatcher, View.OnClickListener {
    private EditText           mIssuer;
    private EditText           mLabel;
    private ImageButton        mImage;
    private Button             mRestore;
    private Button             mSave;

    private String mIssuerCurrent;
    private String mIssuerDefault;
    private String mLabelCurrent;
    private String mLabelDefault;
    private Uri mImageCurrent;
    private Uri mImageDefault;
    private Uri mImageDisplay;

    private void showImage(Uri uri) {
        mImageDisplay = uri;
        onTextChanged(null, 0, 0, 0);
        Picasso.with(this)
                .load(uri)
                .placeholder(R.drawable.logo)
                .into(mImage);
    }

    private boolean imageIs(Uri uri) {
        if (uri == null)
            return mImageDisplay == null;

        return uri.equals(mImageDisplay);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        // Get token values.
        Token token = new TokenPersistence(this).get(getPosition());
        mIssuerCurrent = token.getIssuer();
        mLabelCurrent = token.getLabel();
        mImageCurrent = token.getImage();
        token.setIssuer(null);
        token.setLabel(null);
        token.setImage(null);
        mIssuerDefault = token.getIssuer();
        mLabelDefault = token.getLabel();
        mImageDefault = token.getImage();

        // Get references to widgets.
        mIssuer = (EditText) findViewById(R.id.issuer);
        mLabel = (EditText) findViewById(R.id.label);
        mImage = (ImageButton) findViewById(R.id.image);
        mRestore = (Button) findViewById(R.id.restore);
        mSave = (Button) findViewById(R.id.save);

        // Setup text changed listeners.
        mIssuer.addTextChangedListener(this);
        mLabel.addTextChangedListener(this);

        // Setup click callbacks.
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.restore).setOnClickListener(this);
        mImage.setOnClickListener(this);

        // Setup initial state.
        showImage(mImageCurrent);
        mLabel.setText(mLabelCurrent);
        mIssuer.setText(mIssuerCurrent);
        mIssuer.setSelection(mIssuer.getText().length());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
            showImage(data.getData());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String label = mLabel.getText().toString();
        String issuer = mIssuer.getText().toString();
        mSave.setEnabled(!label.equals(mLabelCurrent) || !issuer.equals(mIssuerCurrent) || !imageIs(mImageCurrent));
        mRestore.setEnabled(!label.equals(mLabelDefault) || !issuer.equals(mIssuerDefault) || !imageIs(mImageDefault));
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image:
                startActivityForResult(new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 0);
                break;

            case R.id.restore:
                mLabel.setText(mLabelDefault);
                mIssuer.setText(mIssuerDefault);
                mIssuer.setSelection(mIssuer.getText().length());
                showImage(mImageDefault);
                break;

            case R.id.save:
                TokenPersistence tp = new TokenPersistence(this);
                Token token = tp.get(getPosition());
                token.setIssuer(mIssuer.getText().toString());
                token.setLabel(mLabel.getText().toString());
                token.setImage(mImageDisplay);
                tp.save(token);

            case R.id.cancel:
                finish();
                break;
        }
    }
}

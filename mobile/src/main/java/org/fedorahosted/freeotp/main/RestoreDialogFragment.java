/*
 * FreeOTP
 *
 * Authors: Brad Williams <brawilli@redhat.com>
 *
 * Copyright (C) 2026  Brad Williams, Red Hat
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

package org.fedorahosted.freeotp.main;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.fedorahosted.freeotp.R;

public class RestoreDialogFragment extends DialogFragment implements View.OnClickListener {
    private TextInputEditText mPasswordInput;
    private TextInputLayout mPasswordLayout;
    private Button mOkButton;
    private Button mCancelButton;
    private RestoreDialogListener mListener;

    public interface RestoreDialogListener {
        void onRestorePassword(String password);
        void onRestoreCancel();
    }

    static RestoreDialogFragment newInstance() {
        return new RestoreDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_restore, container, false);

        mPasswordInput = v.findViewById(R.id.restore_password_input);
        mPasswordLayout = v.findViewById(R.id.restore_password_layout);
        mOkButton = v.findViewById(R.id.restore_ok_button);
        mCancelButton = v.findViewById(R.id.restore_cancel_button);

        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);

        // Clear error when user starts typing
        mPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mPasswordLayout.getError() != null) {
                    mPasswordLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle Enter/Go key press
        mPasswordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    mOkButton.performClick();
                    return true;
                }
                return false;
            }
        });

        return v;
    }

    public void setListener(RestoreDialogListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.restore_ok_button) {
            if (mPasswordInput != null && mListener != null) {
                String password = mPasswordInput.getText().toString();

                // Validate password is not empty
                if (password.isEmpty()) {
                    mPasswordLayout.setError(getString(R.string.main_restore_password_required));
                    return;
                }

                mListener.onRestorePassword(password);
            }
        } else if (id == R.id.restore_cancel_button) {
            if (mListener != null) {
                mListener.onRestoreCancel();
            }
        }
    }

    public void clearPassword() {
        if (mPasswordInput != null) {
            mPasswordInput.setText("");
        }
        if (mPasswordLayout != null) {
            mPasswordLayout.setError(null);
        }
    }

    public void showBadPasswordError() {
        if (mPasswordLayout != null) {
            mPasswordLayout.setError(getString(R.string.main_restore_bad_password));
            mPasswordInput.requestFocus();
        }
    }
}

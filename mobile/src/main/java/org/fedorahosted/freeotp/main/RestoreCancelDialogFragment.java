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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.fedorahosted.freeotp.R;

public class RestoreCancelDialogFragment extends DialogFragment implements View.OnClickListener {
    private TextView mGoBackButton;
    private TextView mProceedButton;
    private RestoreCancelDialogListener mListener;

    public interface RestoreCancelDialogListener {
        void onGoBack();
        void onProceed();
    }

    static RestoreCancelDialogFragment newInstance() {
        return new RestoreCancelDialogFragment();
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
        View v = inflater.inflate(R.layout.fragment_restore_cancel, container, false);

        mGoBackButton = v.findViewById(R.id.restore_cancel_go_back_button);
        mProceedButton = v.findViewById(R.id.restore_cancel_proceed_button);

        mGoBackButton.setOnClickListener(this);
        mProceedButton.setOnClickListener(this);

        return v;
    }

    public void setListener(RestoreCancelDialogListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.restore_cancel_go_back_button) {
            if (mListener != null) {
                mListener.onGoBack();
            }
            dismiss();
        } else if (id == R.id.restore_cancel_proceed_button) {
            if (mListener != null) {
                mListener.onProceed();
            }
            dismiss();
        }
    }
}

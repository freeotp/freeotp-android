/*
 * FreeOTP
 *
 * Authors: Justin Stephenson <jstephen@redhat.com>
 *
 * Copyright (C) 2022  Justin Stephenson, Red Hat
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.R;


public class EditTokenDialogFragment extends DialogFragment implements View.OnClickListener {
    private EditText mAccount;
    private EditText mIssuer;
    private Button mSave;

    static EditTokenDialogFragment newInstance(String account, String issuer, int image_id, String image_url, int color) {
        EditTokenDialogFragment f = new EditTokenDialogFragment();

        Bundle args = new Bundle();
        args.putString("account", account);
        args.putString("issuer", issuer);
        args.putInt("image_id", image_id);
        args.putString("image_url", image_url);
        args.putInt("color", color);
        f.setArguments(args);

        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = View.inflate(getContext(), R.layout.fragment_edit, null);

        mAccount = v.findViewById(R.id.fragment_edit_text_account);
        mIssuer = v.findViewById(R.id.fragment_edit_text_issuer);
        ImageButton mIcon = v.findViewById(R.id.fragment_icon_image_button);
        mSave = v.findViewById(R.id.fragment_save_button);

        mSave.setOnClickListener((View.OnClickListener) this);
        if (getArguments() != null) {
            mAccount.setText(getArguments().getString("account"));
            mIssuer.setText(getArguments().getString("issuer"));
        }

        mIcon.setBackgroundColor(getArguments().getInt("color"));

        int image_id = getArguments().getInt("image_id");
        String image_url = getArguments().getString("image_url");
        if (image_url == null) {
            mIcon.setImageResource(getArguments().getInt("image_id"));
        } else {
            Picasso.get().load(image_url).error(image_id).into(mIcon);
        }
        return v;
    }

    public void onClick(View v) {
        String account = mAccount.getText().toString();
        String issuer = mIssuer.getText().toString();

        if (v.getId() == R.id.fragment_save_button) {
            Bundle result = new Bundle();
            result.putString("account", account);
            result.putString("issuer", issuer);
            getParentFragmentManager().setFragmentResult("requestKey", result);
            dismiss();
        }
    }
}
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.databinding.FragmentEditBinding;

public class EditTokenDialogFragment extends DialogFragment {
    FragmentEditBinding mBinding;

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
        mBinding = FragmentEditBinding.inflate(inflater);

        mBinding.save.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString("account", mBinding.account.getText().toString());
            result.putString("issuer", mBinding.issuer.getText().toString());
            getParentFragmentManager().setFragmentResult("requestKey", result);
            dismiss();
        });

        if (getArguments() != null) {
            mBinding.account.setText(getArguments().getString("account"));
            mBinding.issuer.setText(getArguments().getString("issuer"));
        }

        mBinding.image.setBackgroundColor(getArguments().getInt("color"));

        int imageId = getArguments().getInt("image_id");
        String imageUrl = getArguments().getString("image_url");
        if (imageUrl == null) {
            mBinding.image.setImageResource(getArguments().getInt("image_id"));
        } else {
            Picasso.get().load(imageUrl).error(imageId).into(mBinding.image);
        }

        return mBinding.getRoot();
    }
}
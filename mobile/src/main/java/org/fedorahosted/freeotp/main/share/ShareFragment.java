/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
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

package org.fedorahosted.freeotp.main.share;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.fedorahosted.freeotp.R;

import androidx.recyclerview.widget.RecyclerView;

public class ShareFragment extends BottomSheetDialogFragment implements Discoverable.DiscoveryCallback {
    public static final String CODE_ID = "CODE";

    private final Adapter mShareTokenAdapter = new Adapter();
    private Discoverable[] mDiscoverables;
    private String mCode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mCode = getArguments().getString(CODE_ID);
        mDiscoverables = new Discoverable[] {
                new Clipboard(getContext(), this),
                new Jelling(getContext(), this),
        };

        View v = View.inflate(getContext(), R.layout.fragment_share, null);

        RecyclerView rv = v.findViewById(R.id.targets);
        rv.setAdapter(mShareTokenAdapter);
        rv.setHasFixedSize(false);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        for (Discoverable d : mDiscoverables) {
            if (d.permissions().length == 0)
                onRequestPermissionsResult(0, d.permissions(), new int[0]);
            else if (d.permitted())
                requestPermissions(d.permissions(), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] results) {
        for (Discoverable d : mDiscoverables) {
            if (!d.permitted() || d.isDiscovering())
                continue;

            Intent intent = d.enablement();
            if (intent != null)
                startActivityForResult(intent, 0);
            else
                onActivityResult(0, 0, null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (Discoverable d : mDiscoverables) {
            if (!d.permitted() || d.isDiscovering())
                continue;

            Intent intent = d.enablement();
            if (intent == null)
                d.startDiscovery();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        for (Discoverable d : mDiscoverables) {
            if (d.isDiscovering())
                d.stopDiscovery();
        }
    }

    @Override
    public void onShareAppeared(final Discoverable discoverable, final Adapter.Item item,
                                final Discoverable.Shareable shareable) {
        if (shareable == null) {
            item.setOnClickListener(new Adapter.Item.OnClickListener() {
                @Override
                public void onClick(Adapter.Item item) {
                    requestPermissions(discoverable.permissions(), 0);
                }
            });
        } else {
            item.setOnClickListener(new Adapter.Item.OnClickListener() {
                @Override
                public void onClick(Adapter.Item item) {
                    item.setOnClickListener(null);
                    for (int i = 0; i < mShareTokenAdapter.getItemCount(); i++)
                        mShareTokenAdapter.get(i).setEnabled(false);

                    shareable.share(mCode, new Discoverable.Shareable.ShareCallback() {
                        @Override
                        public void onShareCompleted(boolean success) {
                            dismiss();
                        }
                    });
                }
            });
        }

        mShareTokenAdapter.add(item);
    }

    @Override
    public void onShareDisappeared(Discoverable discoverable, final Adapter.Item item) {
        mShareTokenAdapter.remove(item);
    }
}

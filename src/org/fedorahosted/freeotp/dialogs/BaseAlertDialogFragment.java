/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
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

package org.fedorahosted.freeotp.dialogs;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.view.View;

public abstract class BaseAlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private final int mTitle;
    private final int mLayout;
    private final int mNegative;
    private final int mNeutral;
    private final int mPositive;

    public BaseAlertDialogFragment(int title, int layout, int negative, int neutral, int positive) {
        mTitle = title;
        mLayout = layout;
        mNegative = negative;
        mNeutral = neutral;
        mPositive = positive;
    }

    @Override
    public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle);

        if (mNegative != 0)
            builder.setNegativeButton(mNegative, this);

        if (mNeutral != 0)
            builder.setNeutralButton(mNeutral, this);

        if (mPositive != 0)
            builder.setPositiveButton(mPositive, this);

        View view = getActivity().getLayoutInflater().inflate(mLayout, null, false);
        onViewInflated(view);
        builder.setView(view);

        return builder.create();
    };

    protected abstract void onViewInflated(View view);
}

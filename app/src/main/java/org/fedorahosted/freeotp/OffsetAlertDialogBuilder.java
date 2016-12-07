/*
 * FreeOTP
 *
 * Authors: Christian Mäder <mail a cimnine ch>
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

package org.fedorahosted.freeotp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Displays the dialog to choose the offset
 */

class OffsetAlertDialogBuilder {
    static AlertDialog create(final Context ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        final View offsetPromptView = LayoutInflater.from(ctx).inflate(R.layout.offset, null);
        builder.setView(offsetPromptView);
        OffsetPersistence offsetPersistence = new OffsetPersistence(offsetPromptView.getContext());

        builder.setPositiveButton(R.string.offset_dialog_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                checkAndStore(offsetPromptView);
            }
        });

        final AlertDialog alertDialog = builder.create();

        final EditText offsetInput =
                (EditText) offsetPromptView.findViewById(R.id.offset_in_seconds_input);
        offsetInput.setText(String.format("%d", offsetPersistence.getInSeconds()));
        offsetInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    if (checkAndStore(offsetPromptView)) {
                        alertDialog.dismiss();
                    }
                }
                return false;
            }
        });
        return alertDialog;
    }

    private static boolean checkAndStore(View offsetPromptView) {
        final EditText offsetInput =
                (EditText) offsetPromptView.findViewById(R.id.offset_in_seconds_input);
        final OffsetPersistence offsetPersistence = new OffsetPersistence(offsetPromptView.getContext());

        String input = String.valueOf(offsetInput.getText());

        if (input.matches("^-?\\d+$")) {
            offsetPersistence.store(Integer.parseInt(input));
            return true;
        } else {
            offsetInput.selectAll();
            Toast.makeText(offsetPromptView.getContext(), R.string.offset_accepts_only_digits, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}

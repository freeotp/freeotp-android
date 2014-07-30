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

package org.fedorahosted.freeotp.adapters;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TokenUIBaseAdapter extends TokenPersistenceAdapter {
    private final LayoutInflater mLayoutInflater;

    public TokenUIBaseAdapter(Context ctx) {
        super(ctx);
        mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected void bindView(View view, int position, Token token) {
        TextView label = (TextView) view.findViewById(R.id.label);
        TextView issuer = (TextView) view.findViewById(R.id.issuer);

        label.setText(token.getLabel());
        issuer.setText(token.getIssuer());
        if (issuer.getText().length() == 0) {
            issuer.setText(token.getLabel());
            label.setVisibility(View.GONE);
        } else {
            label.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected View createView(ViewGroup parent, int type) {
        return mLayoutInflater.inflate(R.layout.token, parent, false);
    }
}

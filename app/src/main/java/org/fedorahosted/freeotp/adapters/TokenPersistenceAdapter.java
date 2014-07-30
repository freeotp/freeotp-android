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

package org.fedorahosted.freeotp.adapters;

import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.content.Context;
import android.view.View;

public abstract class TokenPersistenceAdapter extends ReorderableBaseAdapter {
    private final TokenPersistence mTokenPersistence;

    public TokenPersistenceAdapter(Context ctx) {
        mTokenPersistence = new TokenPersistence(ctx);
    }

    @Override
    public int getCount() {
        return mTokenPersistence.length();
    }

    @Override
    public Token getItem(int position) {
        return mTokenPersistence.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    protected void move(int fromPosition, int toPosition) {
        mTokenPersistence.move(fromPosition, toPosition);
        notifyDataSetChanged();
    }

    @Override
    protected void bindView(View view, int position) {
        bindView(view, position, getItem(position));
    }

    protected abstract void bindView(View view, int position, Token token);
}

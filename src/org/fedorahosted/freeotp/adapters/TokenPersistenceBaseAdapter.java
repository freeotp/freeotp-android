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

import java.util.LinkedList;
import java.util.List;

import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.Token.TokenUriInvalidException;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class TokenPersistenceBaseAdapter extends DeleteActionBarBaseAdapter {
    private static final String     NAME  = "tokens";
    private static final String     ORDER = "tokenOrder";
    private final SharedPreferences prefs;

    private List<String> getTokenOrder() {
        try {
            JSONArray array = new JSONArray(prefs.getString(ORDER, null));
            List<String> out = new LinkedList<String>();
            for (int i = 0; i < array.length(); i++)
                out.add(array.getString(i));
            return out;
        } catch (JSONException e) {
        } catch (NullPointerException e) {
        }

        return new LinkedList<String>();
    }

    private SharedPreferences.Editor setTokenOrder(List<String> order) {
        JSONArray array = new JSONArray();
        for (String key : order)
            array.put(key);

        return prefs.edit().putString(ORDER, array.toString());
    }

    public TokenPersistenceBaseAdapter(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    @Override
    public int getCount() {
        return getTokenOrder().size();
    }

    @Override
    public Token getItem(int position) {
        try {
            return new Token(prefs.getString(getTokenOrder().get(position), null));
        } catch (TokenUriInvalidException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void move(int fromPosition, int toPosition) {
        if (fromPosition == toPosition)
            return;

        List<String> order = getTokenOrder();
        if (fromPosition < 0 || fromPosition > order.size())
            return;
        if (toPosition < 0 || toPosition > order.size())
            return;

        order.add(toPosition, order.remove(fromPosition));
        setTokenOrder(order).apply();
        notifyDataSetChanged();
    }

    public void add(String uri) throws TokenUriInvalidException {
        Token token = new Token(uri);
        String key = token.getID();

        if (prefs.contains(key))
            return;

        List<String> order = getTokenOrder();
        order.add(0, key);
        setTokenOrder(order).putString(key, token.toString()).apply();
        notifyDataSetChanged();
    }

    @Override
    public void delete(int position) {
        List<String> order = getTokenOrder();
        String key = order.remove(position);
        setTokenOrder(order).remove(key).apply();
        notifyDataSetChanged();
    }

    protected void save(Token token) {
        prefs.edit().putString(token.getID(), token.toString()).apply();
    }
}

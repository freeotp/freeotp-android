package org.fedorahosted.freeotp;

import java.util.LinkedList;
import java.util.List;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenPersistence {
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

    public TokenPersistence(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public int length() {
        return getTokenOrder().size();
    }

    public Token get(int position) {
        try {
            return new Token(prefs.getString(getTokenOrder().get(position), null), true);
        } catch (TokenUriInvalidException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void add(String uri) throws TokenUriInvalidException {
        Token token = new Token(uri, false);
        String key = token.getID();

        if (prefs.contains(key))
            return;

        List<String> order = getTokenOrder();
        order.add(0, key);
        setTokenOrder(order).putString(key, token.toString()).apply();
    }

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
    }

    public void delete(int position) {
        List<String> order = getTokenOrder();
        String key = order.remove(position);
        setTokenOrder(order).remove(key).apply();
    }

    public void save(Token token) {
        prefs.edit().putString(token.getID(), token.toString()).apply();
    }
}

package org.fedorahosted.freeotp;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class TokenPersistence {
    private static final String NAME  = "tokens";
    private static final String ORDER = "tokenOrder";
    private final SharedPreferences prefs;
    private final Gson gson;

    private List<String> getTokenOrder() {
        Type type = new TypeToken<List<String>>(){}.getType();
        String str = prefs.getString(ORDER, "[]");
        List<String> order = gson.fromJson(str, type);
        return order == null ? new LinkedList<String>() : order;
    }

    private SharedPreferences.Editor setTokenOrder(List<String> order) {
        return prefs.edit().putString(ORDER, gson.toJson(order));
    }

    public static Token addWithToast(Context ctx, String uri) {
        try {
            Token token = new Token(uri);
            new TokenPersistence(ctx).add(token);
            return token;
        } catch (TokenUriInvalidException e) {
            Toast.makeText(ctx, R.string.invalid_token, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return null;
    }

    public TokenPersistence(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public int length() {
        return getTokenOrder().size();
    }

    public Token get(int position) {
        String key = getTokenOrder().get(position);
        String str = prefs.getString(key, null);

        try {
            return gson.fromJson(str, Token.class);
        } catch (JsonSyntaxException jse) {
            // Backwards compatibility for URL-based persistence.
            try {
                return new Token(str, true);
            } catch (TokenUriInvalidException tuie) {
                tuie.printStackTrace();
            }
        }

        return null;
    }

    public void add(Token token) throws TokenUriInvalidException {
        String key = token.getID();

        if (prefs.contains(key))
            return;

        List<String> order = getTokenOrder();
        order.add(0, key);
        setTokenOrder(order).putString(key, gson.toJson(token)).apply();
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
        prefs.edit().putString(token.getID(), gson.toJson(token)).apply();
    }
}

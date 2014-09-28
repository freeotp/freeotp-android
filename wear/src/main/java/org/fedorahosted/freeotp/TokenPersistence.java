package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;


import org.fedorahosted.freeotp.Token.TokenUriInvalidException;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

public class TokenPersistence {
    private static final String NAME = "tokens";
    public static final String ORDER = "tokenOrder";
    public static final String TOKEN_KEY = "org.jboss.aerogear.Token";
    private static final String DEFAULT_TOKEN = "org.jboss.aerogear.DefaultTokenId";
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context ctx;

    private List<String> getTokenOrder() {
        Type type = new TypeToken<List<String>>() {
        }.getType();
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
        this.prefs = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        this.ctx = ctx;
        this.gson = new Gson();
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

    public void sync(Token token, GoogleApiClient mGoogleClient) {
        PutDataMapRequest dataMap = PutDataMapRequest.create("/tokens");
        int length = length();
        dataMap.getDataMap().putString(token.getID(), new Gson().toJson(token));

        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleClient, request);

    }

    public String getDefaultTokenId() {
        return prefs.getString(DEFAULT_TOKEN, "");
    }

    public void setDefaultTokenId(String tokenId) {
        prefs.edit().putString(DEFAULT_TOKEN, tokenId).apply();
    }

    public Token getDefaultToken() {
        String str = prefs.getString(getDefaultTokenId(), null);
        if (str == null) {
            Token token = get(0);
            setDefaultTokenId(token.getID());
            return token;
        }
        return gson.fromJson(str, Token.class);
    }

    public void setOrder(String order) {
        prefs.edit().putString(ORDER, order).apply();
    }

    public void clear() {
        int length = length();
        for (int i = 0; i < length; i++) {
            delete(0);
        }

    }
}

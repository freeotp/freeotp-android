package org.fedorahosted.freeotp;

import java.util.LinkedList;
import java.util.List;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenStore {
	private static final String NAME = "tokens";
	private static final String ORDER = "tokenOrder";
	private SharedPreferences prefs;
	
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
	
	public TokenStore(Context ctx) {
		prefs = ctx.getApplicationContext()
				   .getSharedPreferences(NAME, Context.MODE_PRIVATE);
	}
	
	public void add(Token token) {
		String key = token.getID();
		
		if (prefs.contains(key))
			return;
		
		List<String> order = getTokenOrder();
		order.add(0, key);
		setTokenOrder(order).putString(key, token.toString()).apply();
	}
	
	public void del(int index) {		
		List<String> order = getTokenOrder();
		String key = order.remove(index);
		setTokenOrder(order).remove(key).apply();
	}
	
	public void save(Token token) {	
		prefs.edit().putString(token.getID(), token.toString()).apply();
	}
	
	public Token get(int index) {
		try {
			return new Token(prefs.getString(getTokenOrder().get(index), null));
		} catch (TokenUriInvalidException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public int getTokenCount() {
		return getTokenOrder().size();
	}
	
	public void move(int fromIndex, int toIndex) {
		List<String> order = getTokenOrder();
		if (fromIndex < 0 || fromIndex > order.size())
			return;
		if (toIndex < 0 || toIndex > order.size())
			return;
		
		order.add(toIndex, order.remove(fromIndex));
		setTokenOrder(order).apply();
	}
}

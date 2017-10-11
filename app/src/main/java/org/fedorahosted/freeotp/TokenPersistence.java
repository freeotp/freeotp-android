package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;
import com.bottlerocketstudios.vault.SharedPreferenceVaultRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;

import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;

public class TokenPersistence extends AsyncTask<Context, Void, TokenPersistence> {
    private static final String NAME  = "tokens";
    private static final String ORDER = "tokenOrder";
    private Gson gson;
    private final String PREF_FILE_NAME = "freeotp_pref";
    private final String KEY_FILE_NAME = "freeotp_keyname";
    private final String KEY_ALIAS = "freeotp_keyalias";
    private final int VAULT_ID = 0;
    private SharedPreferenceVault secureVault;
    private SharedPreferences sharedPreferences;

    @Override
    protected TokenPersistence doInBackground(Context... contexts) {
        return new TokenPersistence(contexts[0]);
    }

    private List<String> getTokenOrder(SharedPreferences sharedPreferences) {
        Type type = new TypeToken<List<String>>(){}.getType();
        String str = sharedPreferences.getString(ORDER, "[]");
        List<String> order = gson.fromJson(str, type);
        return order == null ? new LinkedList<String>() : order;
    }

    private SharedPreferences.Editor setTokenOrder(List<String> order) {
        return secureVault.edit().putString(ORDER, gson.toJson(order));
    }

    private boolean hasSharedPrefs(){
        return sharedPreferences.getAll().size() > 0;
    }

    private void migrateToKeyStore(Context ctx) {
        //user has old data in sharedPrefs --> migrate to keystore
        sharedPreferences = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        if(hasSharedPrefs()) {
            List<String> oldTokens = getTokenOrder(sharedPreferences);
            String oldTokenOrder = sharedPreferences.getString(ORDER, "[]");
            secureVault.edit().putString(ORDER, oldTokenOrder).apply();
            for (String currentOldToken : oldTokens) {
                save(gson.fromJson(sharedPreferences.getString(currentOldToken, null), Token.class));
            }
            sharedPreferences.edit().clear().apply();
        }
    }

    private void generateKeyStore(final Context ctx) {
        try {
            //Create an automatically keyed vault
            secureVault = SharedPreferenceVaultFactory.getAppKeyedCompatAes256Vault(
                    ctx,
                    PREF_FILE_NAME,     //Preference file name to store content
                    KEY_FILE_NAME,      //Preference file to store key material
                    KEY_ALIAS,          //App-wide unique key alias
                    VAULT_ID,           //App-wide unique vault id
                    ""                  //String for API < 18
            );
            //Store the created vault in an application-wide store to prevent collisions
            if(SharedPreferenceVaultRegistry.getInstance().getVault(VAULT_ID) != null) {
                SharedPreferenceVaultRegistry.getInstance().addVault(
                        VAULT_ID,
                        PREF_FILE_NAME,
                        KEY_ALIAS,
                        secureVault
                );
            }
        }
        catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

    }

    public TokenPersistence(Context ctx) {
        gson = new Gson();
        generateKeyStore(ctx);
        migrateToKeyStore(ctx);
    }

    public TokenPersistence() {
        // used to instantiate TokenPersistence via AsyncTask
    }

    public int length() {
        return getTokenOrder(secureVault).size();
    }

    public Token get(int position) {
        String key = getTokenOrder(secureVault).get(position);
        String str = secureVault.getString(key, null);

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

        if (secureVault.contains(key))
            return;

        List<String> order = getTokenOrder(secureVault);
        order.add(0, key);
        setTokenOrder(order).putString(key, gson.toJson(token)).apply();
    }

    public void move(int fromPosition, int toPosition) {
        if (fromPosition == toPosition)
            return;

        List<String> order = getTokenOrder(secureVault);
        if (fromPosition < 0 || fromPosition > order.size())
            return;
        if (toPosition < 0 || toPosition > order.size())
            return;

        order.add(toPosition, order.remove(fromPosition));
        setTokenOrder(order).apply();
    }

    public void delete(int position) {
        List<String> order = getTokenOrder(secureVault);
        String key = order.remove(position);
        setTokenOrder(order).remove(key).apply();
    }

    public void save(Token token) {
        secureVault.edit().putString(token.getID(), gson.toJson(token)).apply();
    }
}

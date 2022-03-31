package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import androidx.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.fedorahosted.freeotp.main.Adapter;
import org.fedorahosted.freeotp.utils.SelectableAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;

import javax.crypto.SecretKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TokenPersistenceTest implements SelectableAdapter.EventListener {
    static {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    private Context mContext = new org.fedorahosted.freeotp.Context();
    HashMap<String, String> uris = new HashMap<String, String>();
    int numTokens;
    String pwd = "MyM4sterPassw0rd";

    private void setup(SharedPreferences sp) {
        uris.put("gitlab", "otpauth://totp/gitlab.com:test@redhat.com?secret=nnzl34nyp6ylf5ic43zp5eti2ccgcxuu7ogqflrdzgod7lxo3w4urubx&issuer=gitlab.com");
        uris.put("GitHub", "otpauth://totp/GitHub:testuser?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub");
        uris.put("Red Hat", "otpauth://hotp/OATH38714F5A?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&counter=1&digits=6&issuer=Red%20Hat");
        uris.put("test1", "otpauth://hotp/test1?secret=rvtvsq3l3fgt6dbye76o436vln47q3i45yc227yr4t2hmtmp2el6nzfb&algorithm=SHA256&digits=6&period=30&counter=0");
        uris.put("test2", "otpauth://hotp/test2?secret=7pkajopuoqtxifd5fjx75ymeijowdeuablj7q6dzhglhpglskbyswt4e&algorithm=SHA512&digits=8&period=60&counter=0");

        numTokens = uris.entrySet().size();

        if (sp == null) {
            return;
        }

        String order = "[\"gitlab\",\"GitHub\",\"Red Hat\",\"test1\",\"test2\"]";
        sp.edit().putString("tokenOrder", order).commit();

        for (Map.Entry<String, String> entry : uris.entrySet()) {
            sp.edit().putString(entry.getKey(), entry.getValue()).commit();
        }
    }

    private Adapter wipeAndRestore(SharedPreferences tokenStore) throws TokenPersistence.BadPasswordException,
            GeneralSecurityException, IOException {
        tokenStore.edit().clear().commit();
        assertEquals(0, tokenStore.getAll().size());

        Adapter a = new Adapter(mContext, this);
        assertEquals(0, a.getItemCount());
        a.restoreTokens(pwd);

        return a;
    }

    private void validateTokens(SharedPreferences tokenStore, Adapter adapter) throws InvalidKeyException,
            JSONException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        JSONArray order = new JSONArray(tokenStore.getString("tokenOrder", null));
        assertEquals(numTokens, order.length());
        for (int i = 0; i < order.length(); i++) {
            UUID uuid = UUID.fromString(order.getString(i));

            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            assertTrue(ks.containsAlias(uuid.toString()));
            Key key = ks.getKey(uuid.toString(), null);

            Token token = Token.deserialize(tokenStore.getString(uuid.toString(), null));

            assertNotNull(token.getLabel());
            assertNotNull(token.getPeriod());
            assertTrue(token.getCode(key) != null);
        }

        assertEquals(numTokens, adapter.getItemCount());
    }

    @Test
    // Test restoring added and removed tokens
    public void addRemoveTokensBackupRestore() throws GeneralSecurityException,
            IOException, Token.UnsafeUriException, Token.InvalidUriException, TokenPersistence.BadPasswordException, JSONException {
        setup(null);
        SharedPreferences cur = mContext.getSharedPreferences("tokenStore", android.content.Context.MODE_PRIVATE);
        SharedPreferences bkp = mContext.getSharedPreferences("tokenBackup", Context.MODE_PRIVATE);
        TokenPersistence tokenBackup = new TokenPersistence(mContext);
        String pwd = "MyM4sterPassw0rd";

        tokenBackup.provision(pwd);
        Adapter a = new Adapter(mContext, this);

        for (Map.Entry<String, String> entry : uris.entrySet()) {
            Pair<SecretKey, Token> pair = Token.parse(entry.getValue());
            a.add(pair.first, pair.second);
        }

        assertEquals(numTokens, a.getItemCount());

        // Delete 2 tokens
        a.delete(0);
        a.delete(0);
        numTokens -= 2;

        int tokens = bkp.getAll().size() / 2;
        assertEquals(numTokens, a.getItemCount());
        assertEquals(numTokens, tokens);

        Adapter a2 = wipeAndRestore(cur);

        validateTokens(cur, a2);
    }

    @Test
    // Test restoring multiple migrated tokens
    public void tokenCompatBackupRestore() throws GeneralSecurityException, IOException,
            JSONException, TokenPersistence.BadPasswordException {
        TokenPersistence tokenBackup = new TokenPersistence(mContext);
        String pwd = "MyM4sterPassw0rd";


        SharedPreferences old = mContext.getSharedPreferences("tokens", android.content.Context.MODE_PRIVATE);
        SharedPreferences cur = mContext.getSharedPreferences("tokenStore", android.content.Context.MODE_PRIVATE);
        SharedPreferences bkp = mContext.getSharedPreferences("tokenBackup", Context.MODE_PRIVATE);

        setup(old);
        tokenBackup.provision(pwd);
        Adapter a = new Adapter(mContext, this);

        Adapter a2 = wipeAndRestore(cur);

        validateTokens(cur, a2);
    }

    @Override
    public void onSelectEvent(NavigableSet<Integer> selected) {

    }
}

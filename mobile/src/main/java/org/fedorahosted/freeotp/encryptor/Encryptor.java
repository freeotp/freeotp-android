package org.fedorahosted.freeotp.encryptor;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Map;

import javax.crypto.SecretKey;

public class Encryptor {
    private static final String PREFERENCES = "tokenBackup";
    private static final String MASTER = "masterKey";

    private interface Invalid {
        boolean isProvisioned();
        void fix(String pwd) throws GeneralSecurityException, IOException;
    }

    private SharedPreferences mSharedPreferences;
    private KeyStore mKeyStore;

    public Encryptor(Context ctx) throws KeyStoreException {
        mSharedPreferences = ctx.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        mKeyStore = KeyStore.getInstance("AndroidKeyStore");
    }

    public Invalid getInvalid() {
        final boolean backup = false;
        boolean keystore = false;

        try {
            if (mKeyStore.getKey(MASTER, null) != null)
                return null;
        } catch (GeneralSecurityException ignored) {}

        return new Invalid() {
            @Override
            public boolean isProvisioned() {
                return mSharedPreferences.getString(MASTER, null) != null;
            }

            @Override
            public void fix(String pwd) throws GeneralSecurityException, IOException {
                String s = mSharedPreferences.getString(MASTER, null);
                if (s == null) {
                    s = new Gson().toJson(MasterKey.generate(pwd));
                    mSharedPreferences.edit().putString(MASTER, s).apply();
                }

                MasterKey mk = new Gson().fromJson(s, MasterKey.class);
                SecretKey sk = mk.decrypt(pwd);

                KeyProtection kp = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .build();

                mKeyStore.setEntry(MASTER, new KeyStore.SecretKeyEntry(sk), kp);

                for (Map.Entry<String, ?> item : mSharedPreferences.getAll().entrySet()) {
                    String k = item.getKey();
                    Object v = item.getValue();

                    if (k.equals(MASTER))
                        continue;

                    if (!(v instanceof String))
                        continue;

                    EncryptedKey ek = new Gson().fromJson(s, EncryptedKey.class);
                    SecretKey k = ek.decrypt(sk);


                }
            }
        };
    }
}

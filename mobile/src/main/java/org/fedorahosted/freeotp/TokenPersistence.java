package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.fedorahosted.freeotp.encryptor.MasterKey;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class TokenPersistence {
    private static final String BACKUP = "tokenBackup";
    private static final String STORE = "tokenStore";

    private static final String MASTER = "masterKey";
    private static final String ORDER = "tokenOrder";

    private static final String CIPHER = "AES/GCM/NoPadding";

    private static final int ITERATIONS = 100000;
    private static final int KEY_BITS = 256;

    private final SharedPreferences mBackups;
    private final SharedPreferences mTokens;
    private final KeyStore mKeyStore;

    private List<String> getItems() {
        Type type = new TypeToken<LinkedList<String>>(){}.getType();
        String str = mTokens.getString("tokenOrder", "[]");
        new Gson().fromJson(str, type);
    }

    private SharedPreferences.Editor setItems(List<String> items) {
        return mTokens.edit().putString(ORDER, new Gson().toJson(items));
    }

    TokenPersistence(Context ctx)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        mBackups = ctx.getSharedPreferences(BACKUP, Context.MODE_PRIVATE);
        mTokens = ctx.getSharedPreferences(STORE, Context.MODE_PRIVATE);

        mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        mKeyStore.load(null);
    }

    public boolean isProvisioned() {
        return mBackups.getString(MASTER, null) != null;
    }

    public boolean provision(String password)
            throws BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            IOException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, KeyStoreException {
        // Generate a new master key encrypted by the specified password.
        MasterKey mk = MasterKey.generate(password);

        // NOTE: We can't use PURPOSE_WRAP here because there is no wrap-only granularity.
        KeyProtection kp = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .build();

        // Store the master key in the key store restricted to encrypt-only.
        mKeyStore.setEntry(MASTER, new KeyStore.SecretKeyEntry(mk.decrypt(password)), kp);

        // Store the ciphertext of the master key in the user preferences.
        return mBackups.edit().putString(MASTER, new Gson().toJson(mk)).commit();
    }

    public boolean needsRestore() {
        for (String key : mTokens.getAll().keySet()) {
            UUID uuid = UUID.fromString(key);
            if (uuid == null)
                continue;

            mKeyStore
        }
    }

    public void restore(String password) {
    }

    private int add(SecretKey key, Token token, boolean lock)
            throws GeneralSecurityException, IOException {
        String uuid = UUID.randomUUID().toString();

        // Save key.
        mKeyStore.setEntry(uuid, new KeyStore.SecretKeyEntry(key),
                new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
                        .setUserAuthenticationValidityDurationSeconds(token.getPeriod())
                        .setUserAuthenticationRequired(token.getLock() && lock)
                        .build());

        // Save everything else.
        mItems.add(uuid);
        if (!storeItems().putString(uuid, token.serialize()).commit()) {
            mItems.remove(mItems.size() - 1);
            mKeyStore.deleteEntry(uuid);
            throw new IOException();
        }

        return mItems.size() - 1;
    }

    public int add(SecretKey key, Token token)
            throws GeneralSecurityException, IOException {
        return add(key, token, true);
    }

    public void delete(int position)
            throws GeneralSecurityException, IOException {
        String uuid = mItems.remove(position);
        if (!storeItems().remove(uuid).commit()) {
            mItems.add(position, uuid);
            throw new IOException();
        }

        mKeyStore.deleteEntry(uuid);
    }

    public boolean move(int fromPosition, int toPosition) {
        String uuid = mItems.remove(fromPosition);
        mItems.add(toPosition, uuid);
        if (storeItems().commit()) {
            return true;
        }

        uuid = mItems.remove(toPosition);
        mItems.add(fromPosition, uuid);
        return false;
    }
}

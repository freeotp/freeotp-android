package org.fedorahosted.freeotp.encryptor;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;

import com.google.gson.Gson;

import org.fedorahosted.freeotp.Token;

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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {
    private static final String MASTER = "masterKey";
    private KeyStore mKeyStore;

    public Encryptor(Context ctx) throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {
        mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        mKeyStore.load(null);
    }

    public Map<String, EncryptedKey> encryptToken(SecretKey key) throws
            NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, NoSuchPaddingException, IOException {
        Map<String, EncryptedKey> data = new HashMap<>();

        Key mk = null;
        try {
            mk = mKeyStore.getKey(MASTER, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Encrypt token key
        EncryptedKey ekc = EncryptedKey.encrypt((SecretKey) mk, key);

        data.put("key", ekc);
        return data;
    }
}

package org.fedorahosted.freeotp;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;

import com.google.gson.Gson;

import org.fedorahosted.freeotp.encryptor.EncryptedKey;
import org.fedorahosted.freeotp.encryptor.MasterKey;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class TokenPersistence {
    public class RestoredData {
        public SecretKey key;
        public Token token;
        public String uuid;
    }

    public class BadPasswordException extends Exception {
        public BadPasswordException() {
            super("Invalid password");
        }
    }

    private static final String LOGTAG = "TokenPersistence";
    private static final String BACKUP = "tokenBackup";
    private static final String STORE = "tokenStore";
    private static final String MASTER = "masterKey";

    private final SharedPreferences mBackups;
    private final SharedPreferences mTokens;
    private final Context mContext;
    private final KeyStore mKeyStore;

    public TokenPersistence(Context ctx)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        mBackups = ctx.getSharedPreferences(BACKUP, MODE_PRIVATE);
        mTokens = ctx.getSharedPreferences(STORE, MODE_PRIVATE);
        mContext = ctx;

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

    private boolean needsRestore(String restore_uuid) {
        for (String key : mTokens.getAll().keySet()) {
            if (key == null)
                continue;

            if (key == restore_uuid) {
                return false;
            }
        }

        return true;
    }

    // Save backup data to SharedPreferences
    // (Token data)
    //   key: $UUID-token
    //   value: token
    //
    // and
    //
    // (OTP key)
    //   key: UUID
    //   val: JSON Object with format
    //   {
    //       key: encrypted secret key
    //   }
    public void save(String uuid, String token, EncryptedKey key, Boolean tokendata_only) throws JSONException {
        Log.i(LOGTAG, String.format("Saving [%s] token backup", uuid.toString()));
        if (!tokendata_only) {
            String encryptedCodeKey = new Gson().toJson(key);

            JSONObject obj = new JSONObject().put("key", encryptedCodeKey);

            mBackups.edit().putString(uuid, obj.toString()).apply();
        }

        mBackups.edit().putString(uuid.concat("-token"), token).apply();
    }

    public void remove(String uuid) {
        mBackups.edit().remove(uuid).apply();
        mBackups.edit().remove(uuid.concat("-token")).apply();
    }

    public List<RestoredData> restore(String pwd) throws GeneralSecurityException,
            IOException, JSONException, BadPasswordException  {

        ArrayList<RestoredData> tokensList = new ArrayList<>();

        String s = mBackups.getString(MASTER, null);
        if (s == null) {
            s = new Gson().toJson(MasterKey.generate(pwd));
            mBackups.edit().putString(MASTER, s).apply();
        }

        MasterKey mk = new Gson().fromJson(s, MasterKey.class);
        SecretKey sk;
        try {
            sk = mk.decrypt(pwd);
        } catch (Exception e) {
            Log.e(LOGTAG, "Exception", e);
            throw new BadPasswordException();
        }

        for (Map.Entry<String, ?> item : mBackups.getAll().entrySet()) {
            JSONObject obj;
            String uuid = item.getKey();
            Object v = item.getValue();
            RestoredData bkp = new RestoredData();

            Log.i(LOGTAG, String.format("Found [%s] in backup", uuid));
            if (uuid.equals(MASTER) || !needsRestore(uuid) || item.getKey().contains("-token")) {
                Log.i(LOGTAG, String.format("Skipping [%s]", uuid));
                continue;
            }

            if (!(v instanceof String)) {
                Log.i(LOGTAG, String.format("[%s] Not a string", uuid));
                continue;
            }

            Key key = mKeyStore.getKey(uuid, null);
            if (key != null) {
                Log.i(LOGTAG, String.format("Existing token [%s]! Skipping", uuid));
                continue;
            }

            try {
                obj = new JSONObject(v.toString());
            } catch (JSONException e) {
                // Invalid JSON backup data
                Log.e(LOGTAG, "Exception", e);
                continue;
            }

            // Retrieve encrypted backup data from shared preferences
            String tokenData = mBackups.getString(uuid.concat("-token"), null);
            EncryptedKey ekKey = new Gson().fromJson(obj.getString("key"), EncryptedKey.class);

            // Decrypt the token
            SecretKey skKey = ekKey.decrypt(sk);

            // Deserialize token
            Token token = Token.deserialize(tokenData);

            bkp.key = skKey;
            bkp.token = token;
            bkp.uuid = uuid;
            Log.i(LOGTAG, String.format("Added [%s] token to backup list", uuid));
            tokensList.add(bkp);
        }

        return tokensList;
    }

    public void copyBackupToExternal(Uri uri) {
        ObjectOutputStream output = null;
        try {
            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            output = new ObjectOutputStream(fileOutputStream);

            output.writeObject(mBackups.getAll());
            fileOutputStream.close();
            pfd.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "Exception", e);
        }
    }

    public void restoreBackupFromExternal(Uri uri) {
        ObjectInputStream input = null;
        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
                input = new ObjectInputStream(inputStream);
                SharedPreferences.Editor prefEdit = mBackups.edit();
                prefEdit.clear();
                Map<String, ?> entries = (Map<String, ?>) input.readObject();
                for (Map.Entry<String, ?> entry : entries.entrySet()) {
                    Object v = entry.getValue();
                    String key = entry.getKey();

                    if (v instanceof Boolean)
                        prefEdit.putBoolean(key, (Boolean) v);
                    else if (v instanceof Float)
                        prefEdit.putFloat(key, (Float) v);
                    else if (v instanceof Integer)
                        prefEdit.putInt(key, (Integer) v);
                    else if (v instanceof Long)
                        prefEdit.putLong(key, (Long) v);
                    else if (v instanceof String)
                        prefEdit.putString(key, ((String) v));
                }
                prefEdit.commit();
            } catch (IOException | ClassNotFoundException e) {
                Log.e(LOGTAG, "Exception", e);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ex) {
                    Log.e(LOGTAG, "Exception", ex);
                }
            }
    }
}

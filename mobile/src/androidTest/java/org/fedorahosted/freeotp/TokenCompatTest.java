package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.util.Pair;

import junit.framework.TestCase;

import org.fedorahosted.freeotp.main.Adapter;
import org.fedorahosted.freeotp.utils.Base32;
import org.fedorahosted.freeotp.utils.SelectableAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;

import javax.crypto.SecretKey;

@RunWith(Parameterized.class)
public class TokenCompatTest extends TestCase implements SelectableAdapter.EventListener {
    static {
        Looper.prepare();
    }

    private enum Layout { NONE, PATH, PARAM, BOTH };

    private static final Layout[] LAYOUT = { Layout.PARAM, Layout.NONE, Layout.PATH, Layout.BOTH };
    private static final String[] TYPES = { "hotp", "HOTP", "totp", "TOTP" };
    private static final String[] ISSUERS = { "foo", "είμαι πάπια", "Steam" };
    private static final String[] LABELS = { "baz", "είμαι αρκούδα" };
    private static final String[] SECRETS = { "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "gezdgnbvgy3tqojqgezdgnbvgy3tqojq" };
    private static final String[] ALGORITHMS = { null, "SHA1", "SHA224", "SHA256", "SHA384", "SHA512" };
    private static final String[] DIGITS = { null, "6", "7", "8", "9" };
    private static final String[] PERIODS = { null, "30", "60" };
    private static final String[] COUNTERS = { null, "0", "1234143" };
    private static final String[] IMAGES = { null, };
    private static final String[] COLORS = { null, "000000", "FFFFFF" };
    private static final String[] LOCKS = { null, "true", "false" };

    private static Object[][] compose(Object[] ... sets) {
        int size = 1;
        for (int i = 0; i < sets.length; i++)
            size += sets[i].length;

        Object[][] all = new Object[size][sets.length];

        for (int i = 0; i < sets.length; i++)
            all[0][i] = sets[i][0];

        int off = 1;
        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets[i].length; j++, off++) {
                for (int k = 0; k < sets.length; k++) {
                    all[off][k] = i == k ? sets[k][j] : sets[k][0];
                }
            }
        }

        return all;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(compose(LAYOUT, TYPES, ISSUERS, LABELS, SECRETS, ALGORITHMS, DIGITS,
                PERIODS, COUNTERS, IMAGES, COLORS, LOCKS));
    }

    private Context mContext = new org.fedorahosted.freeotp.Context();

    private Layout mLayout;
    private String mType;
    private String mIssuer;
    private String mLabel;
    private String mSecret;
    private String mAlgorithm;
    private String mDigits;
    private String mPeriod;
    private String mCounter;
    private String mImage;
    private String mColor;
    private String mLock;

    private String fix(String str) {
        return "null".equals(str) ? null : str;
    }

    public TokenCompatTest(Layout layout, String type, String issuer, String label, String secret,
                           String algorithm, String digits, String period, String counter,
                           String image, String color, String lock) {
        mLayout = layout;
        mType = fix(type);
        mIssuer = fix(issuer);
        mLabel = fix(label);
        mSecret = fix(secret);
        mAlgorithm = fix(algorithm);
        mDigits = fix(digits);
        mPeriod = fix(period);
        mCounter = fix(counter);
        mImage = fix(image);
        mColor = fix(color);
        mLock = fix(lock);
    }

    private JSONObject makeJson() throws JSONException, Base32.DecodingException {
        JSONObject obj = new JSONObject();

        JSONArray arr = new JSONArray();
        for (byte b : Base32.RFC4648.decode(mSecret.toUpperCase()))
            arr.put((int) b);

        obj.put("counter", mCounter == null ? 0 : Integer.parseInt(mCounter));
        obj.put("period", mPeriod == null ? 30 : Integer.parseInt(mPeriod));
        obj.put("digits", mDigits == null ? 6 : Integer.parseInt(mDigits));
        obj.put("algo", mAlgorithm == null ? "SHA1" : mAlgorithm);
        obj.put("type", mType.toUpperCase());
        obj.put("label", mLabel);
        obj.put("secret", arr);

        switch (mLayout) {
            case NONE:
                break;

            case PATH:
                obj.put("issuerExt", mIssuer);
                break;

            case PARAM:
                obj.put("issuerInt", mIssuer);
                break;

            case BOTH:
                obj.put("issuerInt", mIssuer);
                obj.put("issuerExt", mIssuer);
                break;
        }

        if (mImage != null)
            obj.put("image", mImage);

        return obj;
    }

    private Uri makeUri() {
        Uri.Builder b = new Uri.Builder().scheme("otpauth").authority(mType);
        switch (mLayout) {
            case NONE:
                b.appendEncodedPath(mLabel);
                break;

            case PATH:
                b.appendEncodedPath(mIssuer + ":" + mLabel);
                break;

            case PARAM:
                b.appendEncodedPath(mLabel).appendQueryParameter("issuer", mIssuer);
                break;

            case BOTH:
                b.appendEncodedPath(mIssuer + ":" + mLabel).appendQueryParameter("issuer", mIssuer);
                break;

            default:
                throw new NullPointerException();
        }

        b.appendQueryParameter("secret", mSecret);

        if (mAlgorithm != null)
            b.appendQueryParameter("algorithm", mAlgorithm);
        if (mDigits != null)
            b.appendQueryParameter("digits", mDigits);
        if (mPeriod != null)
            b.appendQueryParameter("period", mPeriod);
        if (mCounter != null)
            b.appendQueryParameter("counter", mCounter);
        if (mImage != null)
            b.appendQueryParameter("image", mImage);
        if (mColor != null)
            b.appendQueryParameter("color", mColor);
        if (mLock != null)
            b.appendQueryParameter("lock", mLock);

        return b.build();
    }

    @Test
    public void uriToken() throws Token.UnsafeUriException, Token.InvalidUriException, InvalidKeyException {
        Pair<SecretKey, Token> pair = Token.parse(makeUri());

        assertEquals(Token.Type.valueOf(mType.toUpperCase()), pair.second.getType());
        assertEquals(mLayout == Layout.NONE ? null : mIssuer, pair.second.getIssuer());
        assertEquals(mLabel, pair.second.getLabel());

        assertEquals(mImage, pair.second.getImage());
        assertEquals(mColor, pair.second.getColor());
        assertEquals(Boolean.valueOf(mLock).booleanValue(), pair.second.getLock());

        assertTrue(pair.second.getCode(pair.first) != null);
    }

    @Test
    public void uriCompat() throws GeneralSecurityException, IOException, JSONException {
        SharedPreferences old = mContext.getSharedPreferences("tokens", Context.MODE_PRIVATE);
        SharedPreferences cur = mContext.getSharedPreferences("tokenStore", Context.MODE_PRIVATE);
        old.edit()
            .clear()
            .putString("tokenOrder", "[\"foo\"]")
            .putString("foo", makeUri().toString())
            .commit();

        Adapter a = new Adapter(mContext, this);

        // Ensure the migration happened.
        assertEquals(0, old.getAll().size());
        assertEquals(2, cur.getAll().size());

        // Make sure tokenOrder is well formed.
        JSONArray order = new JSONArray(cur.getString("tokenOrder", null));
        assertEquals(1, order.length());

        // Make sure it is a UUID.
        UUID uuid = UUID.fromString(order.getString(0));

        // Make sure the secret is stored in the key store.
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        assertTrue(ks.containsAlias(uuid.toString()));
        Key key = ks.getKey(uuid.toString(), null);

        // Make sure that the token is valid.
        Token token = Token.deserialize(cur.getString(uuid.toString(), null));

        // Check token values.
        assertEquals(Token.Type.valueOf(mType.toUpperCase()), token.getType());
        assertEquals(mLayout == Layout.NONE ? null : mIssuer, token.getIssuer());
        assertEquals(mLabel, token.getLabel());
        assertEquals(mImage, token.getImage());
        assertEquals(mColor, token.getColor());
        assertEquals(Boolean.valueOf(mLock).booleanValue(), token.getLock());
        assertTrue(token.getCode(key) != null);
    }

    @Test
    public void jsonCompat() throws GeneralSecurityException, IOException, JSONException, Base32.DecodingException {
        SharedPreferences old = mContext.getSharedPreferences("tokens", Context.MODE_PRIVATE);
        SharedPreferences cur = mContext.getSharedPreferences("tokenStore", Context.MODE_PRIVATE);
        old.edit()
            .clear()
            .putString("tokenOrder", "[\"foo\"]")
            .putString("foo", makeJson().toString())
            .commit();

        Adapter a = new Adapter(mContext, this);

        for (Map.Entry<String, ?> e: old.getAll().entrySet()) {
            System.out.println(String.format("%s = %s", e.getKey(), e.getValue().toString()));
            System.out.flush();
        }

        // Ensure the migration happened.
        assertEquals(0, old.getAll().size());
        assertEquals(2, cur.getAll().size());

        // Make sure tokenOrder is well formed.
        JSONArray order = new JSONArray(cur.getString("tokenOrder", null));
        assertEquals(1, order.length());

        // Make sure it is a UUID.
        UUID uuid = UUID.fromString(order.getString(0));

        // Make sure the secret is stored in the key store.
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        assertTrue(ks.containsAlias(uuid.toString()));
        Key key = ks.getKey(uuid.toString(), null);

        // Make sure that the token is valid.
        Token token = Token.deserialize(cur.getString(uuid.toString(), null));

        // Check token values.
        assertEquals(Token.Type.valueOf(mType.toUpperCase()), token.getType());
        assertEquals(mLayout == Layout.NONE ? null : mIssuer, token.getIssuer());
        assertEquals(mLabel, token.getLabel());
        assertEquals(mImage, token.getImage());
        assertTrue(token.getCode(key) != null);
    }

    @Override
    public void onSelectEvent(NavigableSet<Integer> selected) {

    }
}

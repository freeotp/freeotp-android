/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp;

import android.net.Uri;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import org.fedorahosted.freeotp.utils.Base32;
import org.fedorahosted.freeotp.utils.Time;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Token {
    public static class UnsafeUriException        extends Exception {}
    public static class UnsafeSecretException     extends UnsafeUriException {}
    public static class UnsafeDigitsException     extends UnsafeUriException {}
    public static class UnsafeAlgorithmException  extends UnsafeUriException {}
    
    public static class InvalidUriException       extends Exception {}
    public static class InvalidCounterException   extends InvalidUriException {}
    public static class InvalidDigitsException    extends InvalidUriException {}
    public static class InvalidPeriodException    extends InvalidUriException {}
    public static class InvalidSecretException    extends InvalidUriException {}
    public static class InvalidLabelException     extends InvalidUriException {}
    public static class InvalidAlgorithmException extends InvalidUriException {}
    public static class InvalidSchemeException    extends InvalidUriException {}
    public static class InvalidTypeException      extends InvalidUriException {}
    public static class InvalidColorException     extends InvalidUriException {}

    public enum Type { HOTP, TOTP }

    private static final String[] SAFE_ALGOS = { "SHA1", "SHA224", "SHA256", "SHA384", "SHA512" };
    private static final Pattern PATTERN = Pattern.compile("^/(?:([^:]+):)?([^:]+)$");

    @SerializedName("algo")
    private final String mAlgorithm;

    @SerializedName("issuerExt")
    private final String mIssuer;

    @SerializedName("issuerInt")
    private final String mIssuerParam;

    @SerializedName("label")
    private final String mLabel;

    @SerializedName("image")
    private final String mImage;

    @SerializedName("color")
    private final String mColor;

    @SerializedName("lock")
    private final Boolean mLock;

    @SerializedName("period")
    private final Integer mPeriod;

    @SerializedName("digits")
    private final Integer mDigits;

    @SerializedName("type")
    private final Type mType;

    @SerializedName("counter")
    private Long mCounter;

    private class Secret {
        private byte[] secret;
    }

    public static Token deserialize(String json) {
        return new Gson().fromJson(json, Token.class);
    }

    public static Pair<SecretKey, Token> compat(String str) throws InvalidUriException {
        try {
            Token t = Token.deserialize(str);
            Secret s = new Gson().fromJson(str, Secret.class);
            SecretKey key = new SecretKeySpec(s.secret, "Hmac" + t.getAlgorithm());
            return new Pair<SecretKey, Token>(key, t);
        } catch (JsonSyntaxException e) {
            // Backwards compatibility for URL-based persistence.
            return Token.parseUnsafe(str);
        }
    }

    public static Pair<SecretKey, Token> parse(String uri) throws UnsafeUriException, InvalidUriException {
        return parse(Uri.parse(uri));
    }

    public static Pair<SecretKey, Token> parseUnsafe(String uri) throws InvalidUriException {
        return parseUnsafe(Uri.parse(uri));
    }

    public static Pair<SecretKey, Token> parse(Uri uri) throws UnsafeUriException, InvalidUriException {
        Pair<SecretKey, Token> pair = parseUnsafe(uri);

        // RFC 4226, Section 4, R6
        if (pair.first.getEncoded().length < 16)
            throw new UnsafeSecretException();

        boolean safeAlgo = false;
        for (String algo : SAFE_ALGOS) {
            if (pair.second.getAlgorithm().equals(algo)) {
                safeAlgo = true;
                break;
            }
        }
        if (!safeAlgo)
            throw new UnsafeAlgorithmException();

        if (pair.second.mDigits != null) {
            Code.Factory f = Code.Factory.fromIssuer(pair.second.mIssuer);
            if (pair.second.mDigits < f.getDigitsMin() || pair.second.mDigits > f.getDigitsMax())
                throw new UnsafeDigitsException();
        }

        return pair;
    }

    public static Pair<SecretKey, Token> parseUnsafe(Uri uri) throws InvalidUriException {
        Token t = new Token(uri);

        try {
            String secret = uri.getQueryParameter("secret").toUpperCase(Locale.US);
            byte[] bytes = Base32.RFC4648.decode(secret);
            return new Pair<SecretKey, Token>(new SecretKeySpec(bytes, "Hmac" + t.getAlgorithm()), t);
        } catch (Base32.DecodingException | NullPointerException e) {
            throw new InvalidSecretException();
        }
    }

    public static Pair<SecretKey, Token> random() {
        Random r = ThreadLocalRandom.current();
        Token t = new Token(r);

        byte[] bytes = new byte[16 + r.nextInt(16)];
        r.nextBytes(bytes);
        return new Pair<SecretKey, Token>(new SecretKeySpec(bytes, "Hmac" + t.getAlgorithm()), t);
    }

    private static final String[] ISSUERS = { "Buffer", "Google+", "HootSuite", "Mastodon",
        "Reddit", "Tumbler", "Twitter", "WordPress.com", "FreeIPA", "Facebook", "Steam",
        "Bitbucket", "gitlab.com", "Code Climate", "GitHub", "Launchpad", "Mapbox" };

    private Token(Random r) {
        mIssuer = r.nextInt(5) < 1 ? null : ISSUERS[r.nextInt(ISSUERS.length)];
        mIssuerParam = mIssuer;
        mAlgorithm = SAFE_ALGOS[r.nextInt(SAFE_ALGOS.length)];
        mType = r.nextBoolean() ? Type.TOTP : Type.HOTP;
        mLabel = UUID.randomUUID().toString();
        mPeriod = 5 + r.nextInt(55);
        mCounter = (long) r.nextInt(1000);
        mLock = r.nextBoolean();

        Code.Factory f = Code.Factory.fromIssuer(mIssuer);
        mDigits = f.getDigitsMin() + r.nextInt(f.getDigitsMax() - f.getDigitsMin());

        mImage = null;
        mColor = null;
    }

    private Token(Uri uri) throws InvalidUriException {
        if (uri.getScheme() == null || !uri.getScheme().equals("otpauth"))
            throw new InvalidSchemeException();

        try {
            mType = Type.valueOf(uri.getAuthority().toUpperCase(Locale.US));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidTypeException();
        }

        try {
            Matcher matcher = PATTERN.matcher(uri.getPath());
            if (!matcher.find())
                throw new InvalidLabelException();

            mIssuer = matcher.group(1);
            mLabel = matcher.group(2);
        } catch (NullPointerException e) {
            throw new InvalidLabelException();
        }

        mIssuerParam = uri.getQueryParameter("issuer");

        try {
            mAlgorithm = uri.getQueryParameter("algorithm");
            if (mAlgorithm != null)
                Mac.getInstance("Hmac" + mAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidAlgorithmException();
        }

        if (mType == Type.HOTP) {
            try {
                String s = uri.getQueryParameter("counter");
                mCounter = Long.parseLong(s == null ? "0" : s);
                if (mCounter < 0)
                    throw new InvalidCounterException();
            } catch (NumberFormatException x) {
                throw new InvalidCounterException();
            }
        }

        try {
            String s = uri.getQueryParameter("period");
            mPeriod = s == null ? null : Integer.parseInt(s);
            if (mPeriod != null && mPeriod < 5)
                throw new InvalidPeriodException();
        } catch (NumberFormatException x) {
            throw new InvalidPeriodException();
        }

        try {
            String s = uri.getQueryParameter("digits");
            mDigits = s == null ? null : Integer.parseInt(s);
            if (mDigits != null && mDigits < 0)
                throw new InvalidDigitsException();
        } catch (NumberFormatException x) {
            throw new InvalidDigitsException();
        }

        if (uri.getQueryParameter("lock") != null)
            mLock = uri.getBooleanQueryParameter("lock", false);
        else
            mLock = null;

        mImage = uri.getQueryParameter("image");

        mColor = uri.getQueryParameter("color");
        if (mColor != null && !mColor.matches("^[0-9a-fA-F]{6}$"))
            throw new InvalidColorException();
    }

    private String getAlgorithm() {
        return mAlgorithm == null ? "SHA1" : mAlgorithm;
    }

    public String serialize() {
        return new Gson().toJson(this);
    }
    
    public String getIssuer() {
        return mIssuer == null ? mIssuerParam : mIssuer;
    }

    public String getLabel() {
        return mLabel;
    }

    public int getPeriod() {
        return mPeriod == null ? 30 : mPeriod;
    }

    public String getImage() {
        return mImage;
    }

    public String getColor() {
        return mColor;
    }

    public Type getType() {
        return mType;
    }

    public boolean getLock() {
        return mLock == null ? false : mLock;
    }

    public Code getCode(Key key) throws InvalidKeyException {
        Mac mac;

        // Prepare the input.
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.BIG_ENDIAN);
        switch (mType) {
            case HOTP: bb.putLong(mCounter++); break;
            case TOTP: bb.putLong(Time.INSTANCE.current() / 1000 / getPeriod()); break;
        }

        try {
            mac = Mac.getInstance("Hmac" + getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            mac = null; // This should never happen since we check validity in the constructor.
        }

        // Do the hashing.
        mac.init(key);
        byte[] digest = mac.doFinal(bb.array());

        // Truncate.
        int off = digest[digest.length - 1] & 0xf;
        int code = (digest[off] & 0x7f) << 0x18;
        code |= (digest[off + 1] & 0xff) << 0x10;
        code |= (digest[off + 2] & 0xff) << 0x08;
        code |= (digest[off + 3] & 0xff);

        return Code.Factory.fromIssuer(mIssuer).makeCode(code, mDigits, getPeriod());
    }

    public Uri toUri() {
        return toUri(null);
    }

    public Uri toUri(SecretKey key) {
        Uri.Builder ub = new Uri.Builder().scheme("otpauth");

        ub.authority(mType.toString().toLowerCase());
        ub.appendEncodedPath(mIssuer != null ? mIssuer + ":" + mLabel : mLabel);

        if (key != null)
            ub.appendQueryParameter("secret", Base32.RFC4648.encode(key.getEncoded()));

        if (mAlgorithm != null)
            ub.appendQueryParameter("algorithm", mAlgorithm);

        if (mPeriod != null)
            ub.appendQueryParameter("period", Integer.toString(mPeriod));

        if (mDigits != null)
            ub.appendQueryParameter("digits", Integer.toString(mDigits));

        if (mLock != null)
            ub.appendQueryParameter("lock", Boolean.toString(mLock));

        if (mColor != null)
            ub.appendQueryParameter("color", mColor);

        if (mImage != null)
            ub.appendQueryParameter("image", mImage);

        if (mType == Type.HOTP)
            ub.appendQueryParameter("counter", Long.toString(mCounter));

        return ub.build();
    }
}

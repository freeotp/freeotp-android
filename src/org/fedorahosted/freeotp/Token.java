/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;

import com.google.android.apps.authenticator.Base32String;
import com.google.android.apps.authenticator.Base32String.DecodingException;

public class Token {
    public static class TokenUriInvalidException extends Exception {
        private static final long serialVersionUID = -1108624734612362345L;
    }

    public static enum TokenType {
        HOTP, TOTP
    }

    private final String mIssuerInt;
    private final String mIssuerExt;
    private String       mIssuerAlt;
    private final String mLabel;
    private String       mLabelAlt;
    private TokenType    mType;
    private String       mAlgorithm;
    private byte[]       mSecret;
    private int          mDigits;
    private long         mCounter;
    private int          mPeriod;

    private Token(Uri uri, boolean internal) throws TokenUriInvalidException {
        if (!uri.getScheme().equals("otpauth"))
            throw new TokenUriInvalidException();

        if (uri.getAuthority().equals("totp"))
            mType = TokenType.TOTP;
        else if (uri.getAuthority().equals("hotp"))
            mType = TokenType.HOTP;
        else
            throw new TokenUriInvalidException();

        String path = uri.getPath();
        if (path == null)
            throw new TokenUriInvalidException();

        // Strip the path of its leading '/'
        for (int i = 0; path.charAt(i) == '/'; i++)
            path = path.substring(1);
        if (path.length() == 0)
            throw new TokenUriInvalidException();

        int i = path.indexOf(':');
        mIssuerExt = i < 0 ? "" : path.substring(0, i);
        mIssuerInt = uri.getQueryParameter("issuer");
        mLabel = path.substring(i >= 0 ? i + 1 : 0);

        mAlgorithm = uri.getQueryParameter("algorithm");
        if (mAlgorithm == null)
            mAlgorithm = "sha1";
        mAlgorithm = mAlgorithm.toUpperCase(Locale.US);
        try {
            Mac.getInstance("Hmac" + mAlgorithm);
        } catch (NoSuchAlgorithmException e1) {
            throw new TokenUriInvalidException();
        }

        try {
            String d = uri.getQueryParameter("digits");
            if (d == null)
                d = "6";
            mDigits = Integer.parseInt(d);
            if (mDigits != 6 && mDigits != 8)
                throw new TokenUriInvalidException();
        } catch (NumberFormatException e) {
            throw new TokenUriInvalidException();
        }

        try {
            String p = uri.getQueryParameter("period");
            if (p == null)
                p = "30";
            mPeriod = Integer.parseInt(p);
        } catch (NumberFormatException e) {
            throw new TokenUriInvalidException();
        }

        if (mType == TokenType.HOTP) {
            try {
                String c = uri.getQueryParameter("counter");
                if (c == null)
                    c = "0";
                mCounter = Long.parseLong(c) - 1;
            } catch (NumberFormatException e) {
                throw new TokenUriInvalidException();
            }
        }

        try {
            String s = uri.getQueryParameter("secret");
            mSecret = Base32String.decode(s);
        } catch (DecodingException e) {
            throw new TokenUriInvalidException();
        }

        if (internal) {
            setIssuer(uri.getQueryParameter("issueralt"));
            setLabel(uri.getQueryParameter("labelalt"));
        }
    }

    private String getHOTP(long counter) {
        // Encode counter in network byte order
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(counter);

        // Create digits divisor
        int div = 1;
        for (int i = mDigits; i > 0; i--)
            div *= 10;

        // Create the HMAC
        try {
            Mac mac = Mac.getInstance("Hmac" + mAlgorithm);
            mac.init(new SecretKeySpec(mSecret, "Hmac" + mAlgorithm));

            // Do the hashing
            byte[] digest = mac.doFinal(bb.array());

            // Truncate
            int binary;
            int off = digest[digest.length - 1] & 0xf;
            binary = (digest[off + 0] & 0x7f) << 0x18;
            binary |= (digest[off + 1] & 0xff) << 0x10;
            binary |= (digest[off + 2] & 0xff) << 0x08;
            binary |= (digest[off + 3] & 0xff) << 0x00;
            binary = binary % div;

            // Zero pad
            String hotp = Integer.toString(binary);
            while (hotp.length() != mDigits)
                hotp = "0" + hotp;

            return hotp;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    public Token(String uri, boolean internal) throws TokenUriInvalidException {
        this(Uri.parse(uri), internal);
    }

    public String getID() {
        String id;
        if (mIssuerInt != null && !mIssuerInt.equals(""))
            id = mIssuerInt + ":" + mLabel;
        else if (mIssuerExt != null && !mIssuerExt.equals(""))
            id = mIssuerExt + ":" + mLabel;
        else
            id = mLabel;

        return id;
    }

    // NOTE: This changes internal data. You MUST save the token immediately.
    public void setIssuer(String issuer) {
        mIssuerAlt = issuer;
    }

    public String getIssuer() {
        if (mIssuerAlt != null)
            return mIssuerAlt;
        return mIssuerExt != null ? mIssuerExt : "";
    }

    // NOTE: This changes internal data. You MUST save the token immediately.
    public void setLabel(String label) {
        mLabelAlt = label;
    }

    public String getLabel() {
        if (mLabelAlt != null)
            return mLabelAlt;
        return mLabel != null ? mLabel : "";
    }

    public int getDigits() {
        return mDigits;
    }

    // NOTE: This may change internal data. You MUST save the token immediately.
    public TokenCode generateCodes() {
        long cur = System.currentTimeMillis();

        switch (mType) {
        case HOTP:
            return new TokenCode(getHOTP(mCounter++), cur, cur + (mPeriod * 1000));

        case TOTP:
            long counter = cur / 1000 / mPeriod;
            return new TokenCode(getHOTP(counter + 0),
                                 (counter + 0) * mPeriod * 1000,
                                 (counter + 1) * mPeriod * 1000,
                   new TokenCode(getHOTP(counter + 1),
                                 (counter + 1) * mPeriod * 1000,
                                 (counter + 2) * mPeriod * 1000));
        }

        return null;
    }

    public TokenType getType() {
        return mType;
    }

    public Uri toUri() {
        String issuerLabel = !mIssuerExt.equals("") ? mIssuerExt + ":" + mLabel : mLabel;

        Uri.Builder builder = new Uri.Builder().scheme("otpauth").path(issuerLabel)
                .appendQueryParameter("secret", Base32String.encode(mSecret))
                .appendQueryParameter("issuer", mIssuerInt == null ? mIssuerExt : mIssuerInt)
                .appendQueryParameter("algorithm", mAlgorithm)
                .appendQueryParameter("digits", Integer.toString(mDigits))
                .appendQueryParameter("period", Integer.toString(mPeriod));

        if (mIssuerAlt != null)
            builder.appendQueryParameter("issueralt", mIssuerAlt);
        if (mLabelAlt != null)
            builder.appendQueryParameter("labelalt", mLabelAlt);

        switch (mType) {
        case HOTP:
            builder.authority("hotp");
            builder.appendQueryParameter("counter", Long.toString(mCounter + 1));
            break;
        case TOTP:
            builder.authority("totp");
            break;
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return toUri().toString();
    }
}

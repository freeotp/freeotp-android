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

import androidx.annotation.Nullable;
import org.fedorahosted.freeotp.utils.Time;

public class Code {
    static class Factory {
        // See: RFC 4226, Section 4, R4
        private static final double RFC_MIN = Math.pow(10, 6);
        private static final double RFC_MAX = Integer.MAX_VALUE;

        static Factory fromIssuer(String issuer) {
            if (issuer == null)
                return new Factory("0123456789");

            if (issuer.equals("Steam"))
                return new Factory("23456789BCDFGHJKMNPQRTVWXY");

            return new Factory("0123456789");
        }

        private final char[] mAlphabet;
        private final int mDigits;

        private Factory(String alphabet) {
            mAlphabet = alphabet.toCharArray();
            mDigits = getDigitsMin();
        }

        int getDigitsMin() {
            return (int) Math.ceil(Math.log(RFC_MIN) / Math.log(mAlphabet.length));
        }

        int getDigitsMax() {
            return (int) Math.floor(Math.log(RFC_MAX) / Math.log(mAlphabet.length));
        }

        Code makeHotpCode(int code, @Nullable Integer digits, int period) {
            return makeCode(code, digits, period, false);
        }

        Code makeTotpCode(int code, @Nullable Integer digits, int period) {
            return makeCode(code, digits, period, true);
        }

        private Code makeCode(int code, @Nullable Integer digits, int period, boolean alignToWindow) {
            if (digits == null)
                digits = mDigits;

            char[] buffer = new char[digits];

            for (int i = 0; i < digits; i++) {
                buffer[digits - i - 1] = mAlphabet[code % mAlphabet.length];
                code /= mAlphabet.length;
            }

            return new Code(new String(buffer), period, alignToWindow);
        }
    }

    private final String mCode;
    private final long mPeriod;
    private final long mStart;
    private final long mWindowStart; // For TOTP: the epoch window this code is valid for

    public Code(String code, long period) {
        this(code, period, false);
    }

    public Code(String code, long period, boolean alignToWindow) {
        long currentTime = Time.INSTANCE.current();
        mPeriod = period * 1000;
        if (alignToWindow) {
            // For TOTP: track which window this code is valid for
            mStart = 0; // Flag to indicate TOTP mode
            mWindowStart = (currentTime / mPeriod) * mPeriod;
        } else {
            // For HOTP: use current time when code was generated
            mStart = currentTime;
            mWindowStart = 0; // Not used for HOTP
        }
        mCode = code;
    }

    public String getCode() {
        return mCode;
    }

    public long timeValid() {
        return mPeriod;
    }

    public long timeLeft() {
        long now = Time.INSTANCE.current();
        if (mStart == 0) {
            // TOTP mode: calculate time until next period boundary based on epoch
            long windowStart = (now / mPeriod) * mPeriod;
            long left = windowStart + mPeriod - now;
            return left < 0 ? 0 : left;
        } else {
            // HOTP mode: calculate time since code was generated
            long left = mStart + mPeriod - now;
            return left < 0 ? 0 : left;
        }
    }

    public int getProgress(int max) {
        return (int) (timeLeft() * max / timeValid());
    }

    public boolean isValid() {
        if (mStart == 0) {
            // TOTP mode: check if we're still in the same window
            long now = Time.INSTANCE.current();
            long currentWindowStart = (now / mPeriod) * mPeriod;
            return currentWindowStart == mWindowStart;
        } else {
            // HOTP mode: check if time hasn't expired
            return timeLeft() > 0;
        }
    }
}

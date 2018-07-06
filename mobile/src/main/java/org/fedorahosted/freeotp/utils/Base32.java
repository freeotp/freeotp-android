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

package org.fedorahosted.freeotp.utils;

public class Base32 {
    public static class DecodingException extends Exception {}

    public final static Base32 RFC4648
            = new Base32("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567");

    private char[] alphabet;

    private Base32(String alphabet) {
        this.alphabet = alphabet.toCharArray();
        assert alphabet.length() == 32;
    }

    public int encodedLength(int declen) {
        return (declen * 8 + 4) / 5;
    }

    public int decodedLength(int enclen) {
        return enclen * 5 / 8;
    }

    public String encode(byte[] decoded) {
        StringBuilder sb = new StringBuilder(encodedLength(decoded.length));
        int carry = 0;
        int bits = 0;

        for (int i = 0; i < decoded.length; i++) {
            bits += 8;
            carry <<= 8;
            carry |= decoded[i];

            while (bits >= 5) {
                sb.append(alphabet[carry >> bits - 5 & 0b00011111]);
                bits -= 5;
            }
        }

        if (bits > 0) {
            sb.append(alphabet[carry << 5 - bits & 0b00011111]);
        }

        return sb.toString();
    }

    private int find(char c) throws DecodingException {
        for (int i = 0; i < alphabet.length; i++)
            if (c == alphabet[i])
                return i;

        throw new DecodingException();
    }

    public byte[] decode(String encoded) throws DecodingException {
        byte[] decoded = new byte[decodedLength(encoded.length())];
        int carry = 0;
        int bits = 0;
        int i = 0;

        for (char c : encoded.toCharArray()) {
            bits += 5;
            carry <<= 5;
            carry |= find(c);

            while (bits >= 8) {
                decoded[i++] = (byte) (carry >> bits - 8 & 0xff);
                bits -= 8;
            }
        }

        return decoded;
    }
}
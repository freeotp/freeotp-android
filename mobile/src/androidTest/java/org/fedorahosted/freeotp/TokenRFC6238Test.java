package org.fedorahosted.freeotp;


import android.util.Pair;

import junit.framework.TestCase;

import org.fedorahosted.freeotp.utils.Base32;
import org.fedorahosted.freeotp.utils.Time;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Collection;

import javax.crypto.SecretKey;

@RunWith(Parameterized.class)
public class TokenRFC6238Test extends TestCase {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {          59L, "SHA1",   "94287082", "12345678901234567890" },
            {          59L, "SHA256", "46119246", "12345678901234567890123456789012" },
            {          59L, "SHA512", "90693936", "1234567890123456789012345678901234567890123456789012345678901234" },
            {  1111111109L, "SHA1",   "07081804", "12345678901234567890" },
            {  1111111109L, "SHA256", "68084774", "12345678901234567890123456789012" },
            {  1111111109L, "SHA512", "25091201", "1234567890123456789012345678901234567890123456789012345678901234" },
            {  1111111111L, "SHA1",   "14050471", "12345678901234567890" },
            {  1111111111L, "SHA256", "67062674", "12345678901234567890123456789012" },
            {  1111111111L, "SHA512", "99943326", "1234567890123456789012345678901234567890123456789012345678901234" },
            {  1234567890L, "SHA1",   "89005924", "12345678901234567890" },
            {  1234567890L, "SHA256", "91819424", "12345678901234567890123456789012" },
            {  1234567890L, "SHA512", "93441116", "1234567890123456789012345678901234567890123456789012345678901234" },
            {  2000000000L, "SHA1",   "69279037", "12345678901234567890" },
            {  2000000000L, "SHA256", "90698825", "12345678901234567890123456789012" },
            {  2000000000L, "SHA512", "38618901", "1234567890123456789012345678901234567890123456789012345678901234" },
            { 20000000000L, "SHA1",   "65353130", "12345678901234567890" },
            { 20000000000L, "SHA256", "77737706", "12345678901234567890123456789012" },
            { 20000000000L, "SHA512", "47863826", "1234567890123456789012345678901234567890123456789012345678901234" },
        });
    }

    private final String mAlgorithm;
    private final String mSecret;
    private final String mCode;
    private final long mTime;

    public TokenRFC6238Test(long time, String algorithm, String code, String secret) {
        mAlgorithm = algorithm;
        mSecret = secret;
        mCode = code;
        mTime = time;
    }

    @Test
    public void test() throws Token.InvalidUriException, InvalidKeyException {
        // Note: we are implicitly testing default period = 30
        String fmt = "otpauth://totp/foo?secret=%s&digits=8&algorithm=%s";
        String uri = String.format(fmt, Base32.RFC4648.encode(mSecret.getBytes()), mAlgorithm);
        Pair<SecretKey, Token> pair = Token.parseUnsafe(uri);

        Time.INSTANCE = new Time() {
            @Override
            public long current() {
                return mTime * 1000;
            }
        };

        assertEquals(mCode, pair.second.getCode(pair.first).getCode());

        Time.INSTANCE = new Time();
    }
}

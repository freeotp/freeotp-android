package org.fedorahosted.freeotp;


import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import junit.framework.TestCase;

import org.fedorahosted.freeotp.utils.Base32;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.InvalidKeyException;

import javax.crypto.SecretKey;

@RunWith(AndroidJUnit4.class)
public class TokenRFC4226Test extends TestCase {
    @Test
    public void test() throws Token.InvalidUriException, InvalidKeyException {
        // Note: we are implicitly testing defaults of:
        //   * digits = 6
        //   * counter = 0
        //   * algorithm = SHA1
        String fmt = "otpauth://hotp/foo?secret=%s";
        String uri = String.format(fmt, Base32.RFC4648.encode("12345678901234567890".getBytes()));

        Pair<SecretKey, Token> pair = Token.parseUnsafe(uri);
        assertEquals("755224", pair.second.getCode(pair.first).getCode());
        assertEquals("287082", pair.second.getCode(pair.first).getCode());
        assertEquals("359152", pair.second.getCode(pair.first).getCode());
        assertEquals("969429", pair.second.getCode(pair.first).getCode());
        assertEquals("338314", pair.second.getCode(pair.first).getCode());
        assertEquals("254676", pair.second.getCode(pair.first).getCode());
        assertEquals("287922", pair.second.getCode(pair.first).getCode());
        assertEquals("162583", pair.second.getCode(pair.first).getCode());
        assertEquals("399871", pair.second.getCode(pair.first).getCode());
        assertEquals("520489", pair.second.getCode(pair.first).getCode());
    }
}

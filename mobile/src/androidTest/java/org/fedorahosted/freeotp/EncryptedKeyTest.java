package org.fedorahosted.freeotp;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.TestCase;

import org.fedorahosted.freeotp.encryptor.EncryptedKey;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

@RunWith(AndroidJUnit4.class)
public class EncryptedKeyTest extends TestCase {
    private SecretKey generateKey(String algorithm) throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance(algorithm);
        kg.init(256);
        return kg.generateKey();
    }

    @Test
    public void test() throws GeneralSecurityException, IOException {
        SecretKey tok = generateKey("HmacSHA512");
        SecretKey key = generateKey("AES");

        EncryptedKey ek = EncryptedKey.encrypt(key, tok);
        SecretKey res = ek.decrypt(key);

        Assert.assertArrayEquals(tok.getEncoded(), res.getEncoded());
        assertEquals(tok.getAlgorithm(), res.getAlgorithm());
    }
}

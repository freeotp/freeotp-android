package org.fedorahosted.freeotp;

import androidx.test.runner.AndroidJUnit4;
import junit.framework.TestCase;

import org.fedorahosted.freeotp.encryptor.EncryptedKey;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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

    @Test
    public void testUTF8() throws GeneralSecurityException, IOException {
        String plaintxt = "火尸木火1十寥日烤";
        SecretKey key = generateKey("AES");

        byte[] plaintxt_encoded = plaintxt.getBytes();

        SecretKey plaintxt_key = new SecretKeySpec(plaintxt_encoded, "AES");
        EncryptedKey ek = EncryptedKey.encrypt(key, plaintxt_key);

        SecretKey sk = ek.decrypt(key);
        byte[] decryptedBytes = sk.getEncoded();
        String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);

        assertEquals(plaintxt, decryptedText);
    }

    @Test
    public void testSecretKeyFactoryDerivedCallerKeyRoundTrip()
            throws GeneralSecurityException, IOException {
        PBEKeySpec spec = new PBEKeySpec(
                new char[] {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'},
                "encrypted-key-test-salt".getBytes(StandardCharsets.UTF_8),
                1000,
                256);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey derivedKey = factory.generateSecret(spec);
            SecretKey callerKey = new SecretKeySpec(derivedKey.getEncoded(), "AES");
            SecretKey originalKey = generateKey("HmacSHA512");

            EncryptedKey encryptedKey = EncryptedKey.encrypt(callerKey, originalKey);
            SecretKey decryptedKey = encryptedKey.decrypt(callerKey);

            Assert.assertArrayEquals(originalKey.getEncoded(), decryptedKey.getEncoded());
            assertEquals(originalKey.getAlgorithm(), decryptedKey.getAlgorithm());
        } finally {
            spec.clearPassword();
        }
    }
}

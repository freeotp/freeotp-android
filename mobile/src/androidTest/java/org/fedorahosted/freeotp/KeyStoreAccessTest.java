package org.fedorahosted.freeotp;

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@RunWith(AndroidJUnit4.class)
public class KeyStoreAccessTest extends TestCase {
    @Test
    public void test() throws GeneralSecurityException, IOException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);

        SecretKey sk = kg.generateKey();

        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);

        // NOTE: We can't use PURPOSE_WRAP here because there is no wrap-only granularity.
        KeyProtection kp = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .build();

        ks.setEntry("master", new KeyStore.SecretKeyEntry(sk), kp);

        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            Key key = ks.getKey("master", null);

            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);

            // Encrypt using the KeyStore protected key.
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] ct = c.doFinal(secret);

            GCMParameterSpec gcm = new GCMParameterSpec(128, c.getIV());

            // Decrypt using the in-memory key.
            c.init(Cipher.DECRYPT_MODE, sk, gcm);
            byte[] pt = c.doFinal(ct);
            Assert.assertArrayEquals(pt, secret);

            // Try to decrypt using the KeyStore protected key.
            // This MUST fail for our security model to work...
            c.init(Cipher.DECRYPT_MODE, key, gcm);
            fail("Expected an InvalidKeyException to be thrown");
        } catch (InvalidKeyException e) {
            // Expected...
        } finally {
            ks.deleteEntry("master");
        }
    }
}

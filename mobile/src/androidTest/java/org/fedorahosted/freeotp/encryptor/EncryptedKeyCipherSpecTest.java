package org.fedorahosted.freeotp.encryptor;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedKeyCipherSpecTest {

    @Test
    public void generatedAesKeyRoundTripsThroughEncryptedKey()
            throws BadPaddingException, IOException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey encryptionKey = generateAesKey();
        SecretKey plaintext = generateAesKey();

        EncryptedKey encrypted = EncryptedKey.encrypt(encryptionKey, plaintext);
        SecretKey decrypted = encrypted.decrypt(encryptionKey);

        Assert.assertArrayEquals(plaintext.getEncoded(), decrypted.getEncoded());
    }

    @Test
    public void masterKeyPasswordRecoverySuppliesAesKeyForEncryptedKey()
            throws BadPaddingException, IOException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        String password = "cipher-spec-test-password";
        MasterKey masterKey = MasterKey.generate(password);
        SecretKey recoveredEncryptionKey = masterKey.decrypt(password);
        SecretKey plaintext = generateAesKey();

        EncryptedKey encrypted = EncryptedKey.encrypt(recoveredEncryptionKey, plaintext);
        SecretKey decrypted = encrypted.decrypt(recoveredEncryptionKey);

        Assert.assertArrayEquals(plaintext.getEncoded(), decrypted.getEncoded());
    }

    @Test
    public void legacyEncodedAesKeyDecryptsExistingEncryptedKeyRecord()
            throws BadPaddingException, IOException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey legacyEncryptionKey = generateAesKey();
        SecretKey plaintext = generateAesKey();
        EncryptedKey existingRecord = EncryptedKey.encrypt(legacyEncryptionKey, plaintext);
        SecretKey reconstructedLegacyKey = new SecretKeySpec(
                legacyEncryptionKey.getEncoded(), "AES");

        SecretKey decrypted = existingRecord.decrypt(reconstructedLegacyKey);

        Assert.assertArrayEquals(plaintext.getEncoded(), decrypted.getEncoded());
    }

    private static SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }
}

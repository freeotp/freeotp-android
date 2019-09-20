package org.fedorahosted.freeotp.encryptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptedKey {
    private final byte[] mCipherText;
    private final byte[] mParameters;
    private final String mCipher;
    private final String mToken;

    private EncryptedKey(Cipher cipher, SecretKey token)
            throws BadPaddingException, IllegalBlockSizeException, IOException {
        mCipher = cipher.getAlgorithm();
        mToken = token.getAlgorithm();

        cipher.updateAAD(mToken.getBytes(StandardCharsets.UTF_8));
        mCipherText = cipher.doFinal(token.getEncoded());
        mParameters = cipher.getParameters().getEncoded();
    }

    public static EncryptedKey encrypt(SecretKey key, SecretKey plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return new EncryptedKey(cipher, plaintext);
    }

    public SecretKey decrypt(SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            IllegalBlockSizeException, IOException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        Cipher cipher = Cipher.getInstance(mCipher);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        String algorithm = cipher.getParameters().getAlgorithm();

        AlgorithmParameters ap = AlgorithmParameters.getInstance(algorithm);
        ap.init(mParameters);

        cipher.init(Cipher.DECRYPT_MODE, key, ap);
        cipher.updateAAD(mToken.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(cipher.doFinal(mCipherText), mToken);
    }
}

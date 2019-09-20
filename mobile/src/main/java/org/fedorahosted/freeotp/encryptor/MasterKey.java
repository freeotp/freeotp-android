package org.fedorahosted.freeotp.encryptor;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class MasterKey {
    private static final int ITERATIONS = 100000;
    private static final int BYTES = 32;

    private final EncryptedKey mEncryptedKey;
    private final String mAlgorithm;
    private final int mIterations;
    private final byte[] mSalt;

    private static SecretKeyFactory getSecretKeyFactory() throws NoSuchAlgorithmException {
        try {
            return SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            return SecretKeyFactory.getInstance("PBKDF2withHmacSHA1");
        }
    }

    private MasterKey(PBEKeySpec spec) throws NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException,
            NoSuchPaddingException, InvalidKeySpecException, IOException {
        SecretKeyFactory skf = getSecretKeyFactory();
        SecretKey pwd = skf.generateSecret(spec);

        byte[] raw = new byte[pwd.getEncoded().length];
        new SecureRandom().nextBytes(raw);
        SecretKey key = new SecretKeySpec(raw, "AES");

        mEncryptedKey = EncryptedKey.encrypt(pwd, key);
        mIterations = spec.getIterationCount();
        mAlgorithm = skf.getAlgorithm();
        mSalt = spec.getSalt();
    }

    public static MasterKey generate(String pwd) throws BadPaddingException,
            NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException,
            InvalidKeyException, InvalidKeySpecException, IOException {
        byte[] salt = new byte[BYTES];
        new SecureRandom().nextBytes(salt);
        return new MasterKey(new PBEKeySpec(pwd.toCharArray(), salt, ITERATIONS, BYTES * 8));
    }

    private SecretKey decrypt(PBEKeySpec spec) throws NoSuchAlgorithmException,
            InvalidKeySpecException, IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            IOException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(mAlgorithm);
        SecretKey pwd = skf.generateSecret(spec);
        return mEncryptedKey.decrypt(pwd);
    }

    public SecretKey decrypt(String pwd) throws BadPaddingException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException,
            InvalidKeySpecException, IOException {
        return decrypt(new PBEKeySpec(pwd.toCharArray(), mSalt, mIterations, mSalt.length * 8));
    }
}
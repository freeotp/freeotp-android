package org.fedorahosted.freeotp.encryptor;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

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
import javax.crypto.spec.SecretKeySpec;

public final class MasterKey {
    private static final int ITERATIONS = 100000;
    private static final int BYTES = 32;

    private final EncryptedKey mEncryptedKey;
    private final int mIterations;
    private final byte[] mSalt;

    private static SecretKey deriveKey(char[] password, byte[] salt, int iterations, int keyLength) {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
        gen.init(PBEParametersGenerator.PKCS5PasswordToBytes(password), salt, iterations);
        KeyParameter keyParam = (KeyParameter) gen.generateDerivedParameters(keyLength * 8);
        return new SecretKeySpec(keyParam.getKey(), "AES");
    }

    private MasterKey(char[] password, byte[] salt, int iterations)
            throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, NoSuchPaddingException, IOException {
        SecretKey pwd = deriveKey(password, salt, iterations, BYTES);

        byte[] raw = new byte[BYTES];
        new SecureRandom().nextBytes(raw);
        SecretKey key = new SecretKeySpec(raw, "AES");

        mEncryptedKey = EncryptedKey.encrypt(pwd, key);
        mIterations = iterations;
        mSalt = salt;
    }

    public static MasterKey generate(String pwd) throws BadPaddingException,
            NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException,
            InvalidKeyException, IOException {
        byte[] salt = new byte[BYTES];
        new SecureRandom().nextBytes(salt);
        return new MasterKey(pwd.toCharArray(), salt, ITERATIONS);
    }

    public SecretKey decrypt(String pwd) throws BadPaddingException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException,
            IOException {
        SecretKey derivedKey = deriveKey(pwd.toCharArray(), mSalt, mIterations, BYTES);
        return mEncryptedKey.decrypt(derivedKey);
    }
}

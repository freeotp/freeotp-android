package org.fedorahosted.freeotp;

import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.TestCase;

import org.fedorahosted.freeotp.encryptor.EncryptedKey;
import org.fedorahosted.freeotp.encryptor.MasterKey;
import org.fedorahosted.freeotp.Token;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.AEADBadTagException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@RunWith(AndroidJUnit4.class)
public class MasterKeyTest extends TestCase {
    @Test
    public void test() throws GeneralSecurityException, IOException {
        String pwd = "MyM4sterPassw0rd";
        String wrongpwd = "Incorrectpwd";

        MasterKey orig = MasterKey.generate(pwd);
        MasterKey read = new Gson().fromJson(new Gson().toJson(orig), MasterKey.class);

        Assert.assertArrayEquals(orig.decrypt(pwd).getEncoded(), read.decrypt(pwd).getEncoded());
        assertEquals(orig.decrypt(pwd).getAlgorithm(), "AES");
        assertEquals(read.decrypt(pwd).getAlgorithm(), "AES");

        try {
            orig.decrypt(wrongpwd);
        } catch (AEADBadTagException e) {
            // Expected
            // javax.crypto.AEADBadTagException:
            // error:1e000065:Cipher functions:OPENSSL_internal:BAD_DECRYPT
        }
    }
}
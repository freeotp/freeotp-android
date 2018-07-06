package org.fedorahosted.freeotp;

import junit.framework.TestCase;

import org.fedorahosted.freeotp.utils.Base32;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class Base32Test extends TestCase {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        /* RFC 4648, Section 10 */
        return Arrays.asList(new Object[][] {
            { "", "" },
            { "f", "MY" },
            { "fo", "MZXQ" },
            { "foo", "MZXW6" },
            { "foob", "MZXW6YQ" },
            { "fooba", "MZXW6YTB" },
            { "foobar", "MZXW6YTBOI" },
        });
    }

    private final byte[] mDecoded;
    private final String mEncoded;

    public Base32Test(String decoded, String encoded) {
        mDecoded = decoded.getBytes(StandardCharsets.US_ASCII);
        mEncoded = encoded;
    }

    @Test
    public void encode() {
        assertEquals(mEncoded, Base32.RFC4648.encode(mDecoded));
    }

    @Test
    public void decode() throws Base32.DecodingException {
        Assert.assertArrayEquals(mDecoded, Base32.RFC4648.decode(mEncoded));
    }
}
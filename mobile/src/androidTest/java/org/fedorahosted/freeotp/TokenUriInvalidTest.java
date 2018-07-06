package org.fedorahosted.freeotp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TokenUriInvalidTest extends TestCase {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "foo",
                    Token.InvalidSchemeException.class },
            { "https://google.com",
                    Token.InvalidSchemeException.class },
            { "otpauth://potp",
                    Token.InvalidTypeException.class },
            { "otpauth://totp",
                    Token.InvalidLabelException.class },
            { "otpauth://totp/",
                    Token.InvalidLabelException.class },
            { "otpauth://totp/foo:bar:baz",
                    Token.InvalidLabelException.class },
            { "otpauth://totp/bar",
                    Token.InvalidSecretException.class },
            { "otpauth://totp/bar?secret=00000000000000000000000000",
                    Token.InvalidSecretException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZU",
                    Token.UnsafeSecretException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&algorithm=foo",
                    Token.InvalidAlgorithmException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&algorithm=MD5",
                    Token.UnsafeAlgorithmException.class },
            { "otpauth://hotp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&counter=-1",
                    Token.InvalidCounterException.class },
            { "otpauth://hotp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&counter=18446744073709551615",
                    Token.InvalidCounterException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&digits=-1",
                    Token.InvalidDigitsException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&digits=5",
                    Token.UnsafeDigitsException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&digits=10",
                    Token.UnsafeDigitsException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&period=-1",
                    Token.InvalidPeriodException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&period=4",
                    Token.InvalidPeriodException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&period=2147483648",
                    Token.InvalidPeriodException.class },

            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&color=00000",
                    Token.InvalidColorException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&color=0000000",
                    Token.InvalidColorException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&color=00000000",
                    Token.InvalidColorException.class },
            { "otpauth://totp/bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&color=GHIJKL",
                    Token.InvalidColorException.class },

            /* Test digit bounds with an alternate token code alphabet. */
            { "otpauth://totp/Steam:bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&digits=4",
                    Token.UnsafeDigitsException.class },
            { "otpauth://totp/Steam:bar?secret=GAYTEMZUGU3DOOBZGAYTEMZUGU&digits=7",
                    Token.UnsafeDigitsException.class },
        });
    }

    private final Class<? extends Token.InvalidUriException> mException;
    private final String mUri;

    public TokenUriInvalidTest(String uri, Class<? extends Token.InvalidUriException> exception) {
        mException = exception;
        mUri = uri;
    }

    @Test
    public void test() throws Token.InvalidUriException, Token.UnsafeUriException {
        try {
            Token.parse(mUri);
            Assert.fail("Unexpectedly succeeded.");
        } catch (Exception e) {
            assertEquals(mException, e.getClass());
        }
    }
}
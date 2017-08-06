package org.fedorahosted.freeotp;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenTest {
    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_nullScheme_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn(null);

        new Token(mockUri);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_invalidScheme_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("borked");

        new Token(mockUri);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_nullAuthority_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn(null);

        new Token(mockUri);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_invalidAuthority_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("borked");

        new Token(mockUri);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_nullPath_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn(null);

        new Token(mockUri);
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedPathsTest {
        private String path;

        public ParameterizedPathsTest (String path) {
            this.path = path;
        }

        @Parameterized.Parameters
        public static String[] paths() {
            return new String[]{
                    "//////",
                    "/agda/asdg/sag/aga:AGSDSAgA@@@:://adg",
                    "@email/",
                    ""
            };
        }

        @Test(expected = Token.TokenUriInvalidException.class)
        public void TokenCtor_invalidPath_throwsTokenUriInvalidException() throws Exception {
            Uri mockUri = Mockito.mock(Uri.class);
            when(mockUri.getScheme()).thenReturn("otpauth");
            when(mockUri.getAuthority()).thenReturn("totp");
            when(mockUri.getPath()).thenReturn(path);

            new Token(mockUri);
        }
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_nullIssuer_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn("FreeOTP:joe@cool.com");
        when(mockUri.getQueryParameter("issuer")).thenReturn(null);

        new Token(mockUri);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_invalidIssuer_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn("FreeOTP:joe@cool.com");
        when(mockUri.getQueryParameter("issuer")).thenReturn("borked");

        new Token(mockUri);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_nullSecret_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("secret")).thenReturn(null);

        new Token(mockUri);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_invalidSecret_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn("borked");
        when(mockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("secret")).thenReturn("κόσμε");

        new Token(mockUri);
    }

    @Test
    public void TokenCtor_nullPeriod_setsDefaultValue() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("secret")).thenReturn("foobar");
        when(mockUri.getQueryParameter("period")).thenReturn(null);

        // When period is not defined it should get set to the default value.

        Token token = new Token(mockUri);
        Field f = Token.class.getDeclaredField("period");
        f.setAccessible(true);
        Integer period = (Integer) f.get(token);
        assertEquals((Integer) 30, period);
    }

    @Test
    public void TokenCtor_invalidPeriod_setsDefaultValue() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn("borked");
        when(mockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("secret")).thenReturn("foobar");
        when(mockUri.getQueryParameter("period")).thenReturn("-1");

        // When period is invalid it should get set to the default value.

        Token token = new Token(mockUri);
        Field f = Token.class.getDeclaredField("period");
        f.setAccessible(true);
        Integer period = (Integer) f.get(token);
        assertEquals((Integer) 30, period);
    }

    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_invalidPeriodType_throwsTokenUriInvalidException() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("totp");
        when(mockUri.getPath()).thenReturn("borked");
        when(mockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("secret")).thenReturn("foobar");
        when(mockUri.getQueryParameter("period")).thenReturn("not a number");

        new Token(mockUri);
    }

    @Test
    public void TokenCtor_ZeroPeriod_ShouldNotThrowArithmeticException() throws Exception {
        Token totpToken = TokenTestUtils.mockToken("totp", null, "foo", 0);
        // Shouldn't throw from a divide by zero!
        totpToken.generateCodes();
    }

    @Test
    public void TokenCtor_hotp_nullCounter_setsDefaultValue() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("hotp");
        when(mockUri.getPath()).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("secret")).thenReturn("foobar");
        when(mockUri.getQueryParameter("counter")).thenReturn(null);

        // When counter is not defined it should get set to the default value.

        Token token = new Token(mockUri);
        Field f = Token.class.getDeclaredField("counter");
        f.setAccessible(true);
        Long counter = (Long) f.get(token);
        assertEquals(Long.valueOf(0), counter);
    }

    @Test
    public void TokenCtor_hotp_setsCounterIfValid() throws Exception {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getScheme()).thenReturn("otpauth");
        when(mockUri.getAuthority()).thenReturn("hotp");
        when(mockUri.getPath()).thenReturn("borked");
        when(mockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mockUri.getQueryParameter("secret")).thenReturn("foobar");
        when(mockUri.getQueryParameter("counter")).thenReturn("-999");

        // When counter is invalid it should get set to the default value.

        Token token = new Token(mockUri);
        Field f = Token.class.getDeclaredField("counter");
        f.setAccessible(true);
        Long counter = (Long) f.get(token);
        assertEquals(Long.valueOf(-999), counter);
    }
}

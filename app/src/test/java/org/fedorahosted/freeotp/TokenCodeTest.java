package org.fedorahosted.freeotp;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenCodeTest {

    private Token totpToken, hotpToken;

    @Before
    public void setUp() throws Exception {
        // otpauth://totp/FreeOTP:joe@google.com?secret=JBSWY3DPEHPK3PXP&issuer=FreeOTP
        totpToken = mockToken("totp", null);

        // otpauth://hotp/FreeOTP:joe@google.com?secret=JBSWY3DPEHPK3PXP&issuer=FreeOTP
        hotpToken = mockToken("hotp", "0");
    }

    @Test
    public void getCurrentCode_totpToken_returns6DigitCode() throws Exception {
        String code = totpToken.generateCodes().getCurrentCode();
        assertTrue(code.length() == 6);
    }

    @Test
    public void getTotalProgress_totpToken_returnsValidIntRange() throws Exception {
        int progress = totpToken.generateCodes().getTotalProgress();
        assertTrue(progress > 0);
        assertTrue(progress < 1000);
    }

    @Test
    public void getCurrentProgress_totpToken_returnsValidIntRange() throws Exception {
        int progress = totpToken.generateCodes().getCurrentProgress();
        assertTrue(progress > 0);
        assertTrue(progress < 1000);
    }

    @Test
    public void getCurrentCode_hotpToken_returnsFixedHOTP() throws Exception {
        String code = hotpToken.generateCodes().getCurrentCode();
        System.out.println(code);
        assertTrue(code.length() == 6);
        assertEquals("282760", code);
    }

    // Who tests the test utils?
    public static Token mockToken(String authority, String counter){
       return mockToken(authority, counter, "FreeOTP:joe@google.com");
    }

    public static Token mockToken(String authority, String counter, String id) {
        // https://github.com/google/google-authenticator/wiki/Key-Uri-Format
        Uri mMockUri = Mockito.mock(Uri.class);

        when(mMockUri.getScheme()).thenReturn("otpauth");
        when(mMockUri.getAuthority()).thenReturn(authority);
        when(mMockUri.getPath()).thenReturn(id);
        when(mMockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mMockUri.getQueryParameter("secret")).thenReturn("JBSWY3DPEHPK3PXP");

        if (authority.equals("hotp")) {
            when(mMockUri.getQueryParameter("counter")).thenReturn(counter);
        }

        try {
            return new Token(mMockUri);
        } catch (Token.TokenUriInvalidException e) {
            e.printStackTrace();
        }

        return null;
    }
}
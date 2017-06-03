package org.fedorahosted.freeotp;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenCodeTest {

    @Mock
    Uri mMockTotpUri;

    @Mock
    Uri mMockHotpUri;

    private Token totpToken, hotpToken;

    @Before
    public void setUp() throws Token.TokenUriInvalidException {
        // https://github.com/google/google-authenticator/wiki/Key-Uri-Format

        // otpauth://totp/FreeOTP:joe@google.com?secret=JBSWY3DPEHPK3PXP&issuer=FreeOTP
        when(mMockTotpUri.getScheme()).thenReturn("otpauth");
        when(mMockTotpUri.getAuthority()).thenReturn("totp");
        when(mMockTotpUri.getPath()).thenReturn("FreeOTP:joe@google.com");
        when(mMockTotpUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mMockTotpUri.getQueryParameter("secret")).thenReturn("JBSWY3DPEHPK3PXP");
        totpToken = new Token(mMockTotpUri);

        // otpauth://hotp/FreeOTP:joe@google.com?secret=JBSWY3DPEHPK3PXP&issuer=FreeOTP
        when(mMockHotpUri.getScheme()).thenReturn("otpauth");
        when(mMockHotpUri.getAuthority()).thenReturn("hotp");
        when(mMockHotpUri.getPath()).thenReturn("Example:alice@google.com");
        when(mMockHotpUri.getQueryParameter("issuer")).thenReturn("Example");
        when(mMockHotpUri.getQueryParameter("secret")).thenReturn("JBSWY3DPEHPK3PXP");
        when(mMockHotpUri.getQueryParameter("counter")).thenReturn("0");
        hotpToken = new Token(mMockHotpUri);
    }

    @Test
    public void getCurrentCode_totpToken_returns6DigitCode() {
        String code = totpToken.generateCodes().getCurrentCode();
        assertTrue(code.length() == 6);
    }

    @Test
    public void getTotalProgress_totpToken_returnsValidIntRange() {
        int progress = totpToken.generateCodes().getTotalProgress();
        assertTrue(progress > 0);
        assertTrue(progress < 1000);
    }

    @Test
    public void getCurrentProgress_totpToken_returnsValidIntRange() {
        int progress = totpToken.generateCodes().getCurrentProgress();
        assertTrue(progress > 0);
        assertTrue(progress < 1000);
    }

    @Test
    public void getCurrentCode_hotpToken_returnsFixedHOTP() {
        String code = hotpToken.generateCodes().getCurrentCode();
        System.out.println(code);
        assertTrue(code.length() == 6);
        assertEquals("282760",code);
    }

}
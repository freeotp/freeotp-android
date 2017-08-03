package org.fedorahosted.freeotp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TokenCodeTest {

    private Token totpToken, hotpToken;

    @Before
    public void setUp() throws Exception {
        // otpauth://totp/FreeOTP:joe@google.com?secret=JBSWY3DPEHPK3PXP&issuer=FreeOTP
        totpToken = TokenTestUtils.mockToken("totp", null);

        // otpauth://hotp/FreeOTP:joe@google.com?secret=JBSWY3DPEHPK3PXP&issuer=FreeOTP
        hotpToken = TokenTestUtils.mockToken("hotp", "0");
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

}
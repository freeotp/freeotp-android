package org.fedorahosted.freeotp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TokenTest {
    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_brokenURI_throwsTokenUriInvalidException() throws Exception {
        new Token("otpauth://totp/?secret=...");
    }
    
    @Test
    public void TokenCtor_ZeroPeriod_ShouldNotThrowArithmeticException() throws Exception {
        Token totpToken = TokenTestUtils.mockToken("totp", null, "foo", 0);
        // Shouldn't throw from a divide by zero!
        totpToken.generateCodes();
    }
}

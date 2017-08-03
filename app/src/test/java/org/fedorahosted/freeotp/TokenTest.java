package org.fedorahosted.freeotp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TokenTest {
    @Test(expected = Token.TokenUriInvalidException.class)
    public void TokenCtor_brokenURI_throwsCorrectException() throws Exception {
        new Token("otpauth://totp/?secret=...");
    }
}

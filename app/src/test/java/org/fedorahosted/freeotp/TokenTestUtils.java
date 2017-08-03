package org.fedorahosted.freeotp;

import android.net.Uri;

import org.mockito.Mockito;

import static org.mockito.Mockito.when;

// Who tests the test utils...?
class TokenTestUtils {

    public static Token mockToken(String authority, String counter) {
        return mockToken(authority, counter, "FreeOTP:joe@google.com");
    }

    public static Token mockToken(String authority, String counter, String id) {
        return mockToken(authority, counter, id, null);
    }

    public static Token mockToken(String authority, String counter, String id, Integer period) {
        // https://github.com/google/google-authenticator/wiki/Key-Uri-Format
        Uri mMockUri = Mockito.mock(Uri.class);

        when(mMockUri.getScheme()).thenReturn("otpauth");
        when(mMockUri.getAuthority()).thenReturn(authority);
        when(mMockUri.getPath()).thenReturn(id);
        when(mMockUri.getQueryParameter("issuer")).thenReturn("FreeOTP");
        when(mMockUri.getQueryParameter("secret")).thenReturn("JBSWY3DPEHPK3PXP");

        if (period != null) {
            when(mMockUri.getQueryParameter("period")).thenReturn(String.valueOf(period.intValue()));
        }

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

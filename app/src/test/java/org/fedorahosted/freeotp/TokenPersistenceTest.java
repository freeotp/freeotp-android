package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Hashtable;

import static org.fedorahosted.freeotp.TokenCodeTest.mockToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class TokenPersistenceTest {

    @Mock
    Context mockContext;

    @Mock
    Context mockApplicationContext;

    @Mock
    SharedPreferences mockSharedPreferences;

    private TokenPersistence tp;
    private Token token;
    private Hashtable<String,String> ht;

    @Before
    public void setUp() throws Exception {
        when(mockContext.getApplicationContext()).thenReturn(mockApplicationContext);
        when(mockApplicationContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences);
        tp = new TokenPersistence(mockContext);
        token = mockToken("totp", null);
    }


    @Test
    public void add() throws Exception {
        ht = new Hashtable<>();

        final SharedPreferences.Editor mockEditor = Mockito.mock(SharedPreferences.Editor.class);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(),anyString())).thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) throws Throwable {
                ht.put(invocation.getArgumentAt(0,String.class),invocation.getArgumentAt(1,String.class));
                return mockEditor;
            }
        });

        tp.add(token);
        assertTrue(ht.size() > 0);

        // language=JSON
        String expectedOTP = "{\"issuerInt\":\"FreeOTP\",\"issuerExt\":\"FreeOTP\",\"label\":\"joe@google.com\",\"type\":\"TOTP\",\"algo\":\"SHA1\",\"secret\":[72,101,108,108,111,33,-34,-83,-66,-17],\"digits\":6,\"counter\":0,\"period\":30}";
        assertEquals(expectedOTP,ht.get("FreeOTP:joe@google.com"));

        // language=JSON
        String expectedOrder = "[\"FreeOTP:joe@google.com\"]";
        assertEquals(expectedOrder,ht.get("tokenOrder"));
    }
}
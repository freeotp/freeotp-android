package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.LinkedHashMap;

import static org.fedorahosted.freeotp.TokenCodeTest.mockToken;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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

    private LinkedHashMap<String, String> mockStore;
    private TokenPersistence tokenPersistence;
    private Token mockToken;

    @Before
    public void setUp() throws Exception {
        when(mockContext.getApplicationContext())
            .thenReturn(mockApplicationContext);
        when(mockApplicationContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences);
        tokenPersistence = new TokenPersistence(mockContext);
        mockToken = mockToken("totp", null);

        mockStore = new LinkedHashMap<>();

        final SharedPreferences.Editor mockEditor = Mockito.mock(SharedPreferences.Editor.class);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString()))
            .thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) throws Throwable {
                mockStore.put(invocation.getArgumentAt(0, String.class),
                              invocation.getArgumentAt(1, String.class));
                return mockEditor;
            }
        });
        when(mockEditor.remove(anyString()))
            .thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) throws Throwable {
                mockStore.remove(invocation.getArgumentAt(0, String.class));
                return mockEditor;
            }
        });

        when(mockSharedPreferences.contains(anyString()))
            .thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return mockStore.containsKey(invocation.getArgumentAt(0, String.class));
            }
        });

        when(mockSharedPreferences.getString(anyString(), any(String.class)))
            .thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                try {
                    return mockStore.getOrDefault(invocation.getArgumentAt(0, String.class),
                                                  invocation.getArgumentAt(1, String.class));
                } catch (NullPointerException np) {
                    return mockStore.getOrDefault(invocation.getArgumentAt(0, String.class), null);
                }
            }
        });
    }

    @Test
    public void add_sameTokenTwice_isIdempotent() throws Exception {
        int sizeBefore = mockStore.size();
        tokenPersistence.add(mockToken);
        assertEquals(sizeBefore + 2, mockStore.size());

        JsonObject token = new JsonObject();
        token.addProperty("issuerInt", "FreeOTP");
        token.addProperty("issuerExt", "FreeOTP");
        token.addProperty("label", "joe@google.com");
        token.addProperty("type", "TOTP");
        token.addProperty("algo", "SHA1");
        token.add("secret", new JsonParser().parse("[72,101,108,108,111,33,-34,-83,-66,-17]"));
        token.addProperty("digits", 6);
        token.addProperty("counter", 0);
        token.addProperty("period", 30);

        // language=JSON
        assertEquals(token, new JsonParser().parse(mockStore.get("FreeOTP:joe@google.com")));

        // language=JSON
        JsonArray order = new JsonArray();
        order.add("FreeOTP:joe@google.com");
        assertEquals(order, new JsonParser().parse(mockStore.get("tokenOrder")));

        sizeBefore = mockStore.size();
        tokenPersistence.add(mockToken);
        assertEquals(sizeBefore, mockStore.size());
    }

    @Test
    public void length_ofStoreWith1Token_returns1() throws Exception {
        int sizeBefore = mockStore.size();
        tokenPersistence.add(mockToken);
        assertEquals(sizeBefore + 2, mockStore.size());

        assertEquals(sizeBefore + 1, tokenPersistence.length());
    }

    @Test
    public void get_WithValidPosition_returnsToken() throws Exception {
        tokenPersistence.add(mockToken);
        Token hotpMockToken = mockToken("hotp", "1", "FreeOTP:mail@google.com");
        tokenPersistence.add(hotpMockToken);

        assertEquals(mockToken.getType(), tokenPersistence.get(1).getType());
        assertEquals(mockToken.getID(), tokenPersistence.get(1).getID());
        assertEquals(mockToken.getDigits(), tokenPersistence.get(1).getDigits());
        assertEquals(mockToken.getLabel(), tokenPersistence.get(1).getLabel());
        assertEquals(mockToken.getIssuer(), tokenPersistence.get(1).getIssuer());
        assertEquals(mockToken.getImage(), tokenPersistence.get(1).getImage());

        assertEquals(hotpMockToken.getType(), tokenPersistence.get(0).getType());
        assertEquals(hotpMockToken.getID(), tokenPersistence.get(0).getID());
        assertEquals(hotpMockToken.getDigits(), tokenPersistence.get(0).getDigits());
        assertEquals(hotpMockToken.getLabel(), tokenPersistence.get(0).getLabel());
        assertEquals(hotpMockToken.getIssuer(), tokenPersistence.get(0).getIssuer());
        assertEquals(hotpMockToken.getImage(), tokenPersistence.get(0).getImage());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_WithInvalidPosition_ThrowsIndexOutOfBounds() throws Exception {
        tokenPersistence.get(0);
    }

    @Test
    public void move_fromSamePosition_DoesNothing() throws Exception {
        tokenPersistence.add(mockToken);
        Token hotpMockToken = mockToken("hotp", "1", "FreeOTP:mail@google.com");
        tokenPersistence.add(hotpMockToken);

        tokenPersistence.move(0, 0);
        assertEquals(mockToken.getID(), tokenPersistence.get(1).getID());
        assertEquals(hotpMockToken.getID(), tokenPersistence.get(0).getID());
    }

    @Test
    public void move_twoValidTokens_SwitchesTokenOrder() throws Exception {
        tokenPersistence.add(mockToken);
        Token hotpMockToken = mockToken("hotp", "1", "FreeOTP:mail@google.com");
        tokenPersistence.add(hotpMockToken);

        assertEquals(mockToken.getID(), tokenPersistence.get(1).getID());
        assertEquals(hotpMockToken.getID(), tokenPersistence.get(0).getID());

        tokenPersistence.move(0, 1);
        assertEquals(mockToken.getID(), tokenPersistence.get(0).getID());
        assertEquals(hotpMockToken.getID(), tokenPersistence.get(1).getID());
    }

    @Test
    public void delete_tokenFromValidPosition_removesIt() throws Exception {
        tokenPersistence.add(mockToken);
        Token hotpMockToken = mockToken("hotp", "1", "FreeOTP:mail@google.com");
        tokenPersistence.add(hotpMockToken);

        assertEquals(hotpMockToken.getID(), tokenPersistence.get(0).getID());
        assertEquals(2, tokenPersistence.length());

        tokenPersistence.delete(0);
        assertEquals(mockToken.getID(), tokenPersistence.get(0).getID());
        assertEquals(1, tokenPersistence.length());
    }

    @Test
    public void save_updatingExistingToken_changesCounter() throws Exception {
        Token hotpMockToken = mockToken("hotp", "1", "FreeOTP:mail@google.com");
        tokenPersistence.add(hotpMockToken);

        Token hotpMockTokenUpdated = mockToken("totp", "1", "FreeOTP:mail@google.com");
        tokenPersistence.save(hotpMockTokenUpdated);

        assertEquals(1, tokenPersistence.length());
        assertEquals(Token.TokenType.TOTP, tokenPersistence.get(0).getType());
    }
}
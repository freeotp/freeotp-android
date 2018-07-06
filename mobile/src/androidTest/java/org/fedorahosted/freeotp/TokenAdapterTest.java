package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import junit.framework.TestCase;

import org.fedorahosted.freeotp.main.Adapter;
import org.fedorahosted.freeotp.utils.SelectableAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;

import javax.crypto.SecretKey;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenAdapterTest extends TestCase implements SelectableAdapter.EventListener {
    private static final String HOTP = "otpauth://hotp/foo:bar?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Mock
    private Context mockContext;

    private Map<String, MemorySharedPreferences> mockStore = new HashMap<>();
    private Pair<SecretKey, Token> pair = Token.parseUnsafe(HOTP);

    private String getItem(String item) {
        return mockStore.get("tokenStore").getString(item, null);
    }

    private int getSize() {
        return mockStore.get("tokenStore").getAll().size();
    }

    public TokenAdapterTest() throws Token.InvalidUriException {
    }

    @Before
    public void setup() {
        when(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenAnswer(new Answer<SharedPreferences>() {
                @Override
                public SharedPreferences answer(InvocationOnMock invocation) {
                    String arg0 = invocation.getArgument(0);
                    if (!mockStore.containsKey(arg0))
                        mockStore.put(arg0, new MemorySharedPreferences());
                    return mockStore.get(arg0);
                }
            });
    }

    private Pair<KeyStore, Adapter> reset() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);

        for (String alias : Collections.list(ks.aliases()))
            ks.deleteEntry(alias);

        JSONArray array = new JSONArray();
        for (int i = 0; i < 4; i++)
            array.put(UUID.randomUUID().toString());

        mockStore.clear();
        mockContext.getSharedPreferences("tokenStore", Context.MODE_PRIVATE)
                .edit().putString("tokenOrder", array.toString()).commit();

        return new Pair<>(ks, new Adapter(mockContext, this));
    }

    @Test
    public void addToken() throws GeneralSecurityException, IOException, JSONException {
        Pair<KeyStore, Adapter> state = reset();

        Log.e(getClass().getCanonicalName(), getItem("tokenOrder"));

        JSONArray prev = new JSONArray(getItem("tokenOrder"));
        assertEquals(4, prev.length());

        assertEquals(1, getSize());
        state.second.add(pair.first, pair.second);
        assertEquals(2, getSize());

        JSONArray next = new JSONArray(getItem("tokenOrder"));
        assertEquals(5, next.length());

        String uuid = (String) next.remove(next.length() - 1);
        assertEquals(prev, next);

        assertEquals(pair.second.serialize(), getItem(uuid));

        assertTrue(state.first.containsAlias(uuid));
        assertTrue(!state.first.containsAlias("foo"));
    }

    @Test
    public void deleteToken() throws GeneralSecurityException, IOException, JSONException {
        Pair<KeyStore, Adapter> state = reset();

        Log.e(getClass().getCanonicalName(), getItem("tokenOrder"));

        JSONArray prev = new JSONArray(getItem("tokenOrder"));
        state.second.add(pair.first, pair.second);
        JSONArray next = new JSONArray(getItem("tokenOrder"));

        String uuid = (String) next.remove(next.length() - 1);
        assertEquals(prev, next);

        assertEquals(2, getSize());
        state.second.delete(state.second.getItemCount() - 1);
        assertEquals(1, getSize());
        assertEquals(prev, new JSONArray(getItem("tokenOrder")));

        assertTrue(!mockStore.get("tokenStore").getAll().containsKey(uuid));
        assertTrue(!state.first.containsAlias(uuid));
        assertTrue(!state.first.containsAlias("foo"));
    }

    @Test
    public void moveToken() throws GeneralSecurityException, IOException, JSONException {
        Pair<KeyStore, Adapter> state = reset();

        Log.e(getClass().getCanonicalName(), getItem("tokenOrder"));

        JSONArray prev = new JSONArray(getItem("tokenOrder"));
        JSONArray next = new JSONArray(getItem("tokenOrder"));
        String one = (String) next.remove(1);
        String two = (String) next.get(2);
        next.put(2, one);
        next.put(3, two);

        assertEquals(1, getSize());
        assertEquals(4, prev.length());
        state.second.move(1, 2);
        assertEquals(4, new JSONArray(getItem("tokenOrder")).length());
        assertEquals(1, getSize());

        assertEquals(next, new JSONArray(getItem("tokenOrder")));
    }

    @Test
    public void moveTokenSame() throws GeneralSecurityException, IOException, JSONException {
        Pair<KeyStore, Adapter> state = reset();

        Log.e(getClass().getCanonicalName(), getItem("tokenOrder"));

        JSONArray prev = new JSONArray(getItem("tokenOrder"));

        assertEquals(1, getSize());
        assertEquals(4, prev.length());
        state.second.move(1, 1);
        assertEquals(4, new JSONArray(getItem("tokenOrder")).length());
        assertEquals(1, getSize());

        assertEquals(prev, new JSONArray(getItem("tokenOrder")));
    }

    @Override
    public void onSelectEvent(NavigableSet<Integer> selected) {

    }
}

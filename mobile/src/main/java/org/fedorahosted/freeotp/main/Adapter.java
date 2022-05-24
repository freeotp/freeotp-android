/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.security.keystore.UserNotAuthenticatedException;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.fedorahosted.freeotp.Code;
import org.fedorahosted.freeotp.TokenIcon;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;
import org.fedorahosted.freeotp.encryptor.EncryptedKey;
import org.fedorahosted.freeotp.encryptor.Encryptor;
import org.fedorahosted.freeotp.utils.Base32;
import org.fedorahosted.freeotp.utils.SelectableAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Adapter extends SelectableAdapter<ViewHolder> implements ViewHolder.EventListener {
    private static final String COMPAT = "tokens";
    private static final String ORDER = "tokenOrder";
    private static final String NAME  = "tokenStore";
    private static final Gson GSON = new Gson();

    private final LongSparseArray<Code> mActive = new LongSparseArray<>();
    private final Handler mHandler = new Handler();
    private final Encryptor mEncryptor;
    private TokenPersistence mTokenBackup;

    private final SharedPreferences mSharedPreferences;
    private final List<String> mItems;
    private final KeyStore mKeyStore;
    private Context mContext;

    private SharedPreferences.Editor storeItems() {
        return mSharedPreferences.edit().putString(ORDER, new Gson().toJson(mItems));
    }

    private void compat(Context context) {
        SharedPreferences sp = context.getSharedPreferences(COMPAT, Context.MODE_PRIVATE);
        Type type = new TypeToken<LinkedList<String>>(){}.getType();
        List<String> items = GSON.fromJson(sp.getString(ORDER, "[]"), type);
        int size = items.size();

        for (int i = size; i > 0; i--) {
            String key = items.remove(i - 1);
            String val = sp.getString(key, null);
            if (val == null) {
                if (!sp.edit().putString(ORDER, GSON.toJson(items)).commit())
                    items.add(i - 1, key);
                continue;
            }

            try {
                Pair<SecretKey, Token> pair = Token.compat(val);
                add(pair.first, pair.second, false, null);

            } catch (GeneralSecurityException | IOException | Token.InvalidUriException e) {
                items.add(i - 1, key);
                e.printStackTrace();
                continue;
            }

            if (!sp.edit().putString(ORDER, GSON.toJson(items)).remove(key).commit()) {
                items.add(i - 1, key);
                try { delete(0); }
                catch (GeneralSecurityException | IOException e) { e.printStackTrace(); }
            }
        }

        if (size > 0 && items.size() == 0)
            sp.edit().remove(ORDER).apply();
    }

    public Adapter(Context context, EventListener listener) throws GeneralSecurityException, IOException {
        super(listener);
        setHasStableIds(true);

        mSharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        mKeyStore.load(null);

        Type type = new TypeToken<LinkedList<String>>(){}.getType();
        String str = mSharedPreferences.getString(ORDER, "[]");
        mItems = GSON.fromJson(str, type);

        mEncryptor = new Encryptor(context);

        try {
            mTokenBackup = new TokenPersistence(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        compat(context);
    }

    @Override
    public long getItemId(int position) {
        return UUID.fromString(mItems.get(position)).getMostSignificantBits();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater li = LayoutInflater.from(parent.getContext());
        View v = li.inflate(R.layout.token, parent, false);
        return new ViewHolder(v, this);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.reset();
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        String uuid = mItems.get(position);
        Token token = Token.deserialize(mSharedPreferences.getString(uuid, null));
        TokenIcon token_icon = new TokenIcon(token, mContext);
        Pair <Integer, String> image = token_icon.mImage;
        holder.bind(token, token_icon.mColor, image.first, image.second,
                mActive.get(getItemId(position)), isSelected(position));
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private int add(SecretKey key, Token token, boolean lock, String existingUuid) throws
            GeneralSecurityException, IOException {
        String uuid = existingUuid;
        if (existingUuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        // Save key.
        mKeyStore.setEntry(uuid, new KeyStore.SecretKeyEntry(key),
            new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
                .setUserAuthenticationValidityDurationSeconds(token.getPeriod())
                .setUserAuthenticationRequired(token.getLock() && lock)
                .build());

        // Save everything else.
        mItems.add(uuid);
        if (!storeItems().putString(uuid, token.serialize()).commit()) {
            mItems.remove(mItems.size() - 1);
            mKeyStore.deleteEntry(uuid);
            throw new IOException();
        }

        // Save to Backup
        if (mTokenBackup.isProvisioned()) {
            try {
                Map<String, EncryptedKey> data = mEncryptor.encryptToken(key);
                mTokenBackup.save(uuid, token.serialize(), data.get("key"), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.notifyItemInserted(mItems.size() - 1);
        return mItems.size() - 1;
    }

    public int add(SecretKey key, Token token) throws GeneralSecurityException, IOException {
        return add(key, token, true, null);
    }

    public void delete(int position) throws GeneralSecurityException, IOException {
        String uuid = mItems.remove(position);
        if (!storeItems().remove(uuid).commit()) {
            mItems.add(position, uuid);
            throw new IOException();
        }

        if (mTokenBackup.isProvisioned()) {
            mTokenBackup.remove(uuid);
        }

        notifyItemRemoved(position);
        mKeyStore.deleteEntry(uuid);
    }

    public void move(int fromPosition, int toPosition) {
        String uuid = mItems.remove(fromPosition);
        mItems.add(toPosition, uuid);
        if (storeItems().commit()) {
            notifyItemMoved(fromPosition, toPosition);
            return;
        }

        uuid = mItems.remove(toPosition);
        mItems.add(fromPosition, uuid);
    }

    @Override
    public boolean onSelectionToggled(ViewHolder holder) {
        boolean selected = !isSelected(holder.getAdapterPosition());
        setSelected(holder.getAdapterPosition(), selected);
        return selected;
    }

    public void restoreTokens(String pwd) throws TokenPersistence.BadPasswordException {
        if (mTokenBackup.isProvisioned()) {
            try {
                List<TokenPersistence.RestoredData> list = mTokenBackup.restore(pwd);
                for (TokenPersistence.RestoredData item : list) {
                    add(item.key, item.token, item.token.getLock(), item.uuid);
                }
            } catch (TokenPersistence.BadPasswordException pe) {
                throw pe;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Code getCode(int position)
            throws UserNotAuthenticatedException, KeyPermanentlyInvalidatedException {
        String uuid = mItems.get(position);
        Code code;

        try {
            Token token = Token.deserialize(mSharedPreferences.getString(uuid, null));
            Key key = mKeyStore.getKey(uuid, null);
            code = token.getCode(key);
            mSharedPreferences.edit().putString(uuid, token.serialize()).apply();
        } catch (UserNotAuthenticatedException | KeyPermanentlyInvalidatedException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return new Code("ERROR", 15);
        }

        final Long id = getItemId(position);
        mActive.put(id, code);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Code code = mActive.get(id);
                if (code == null)
                    return;
                if (!code.isValid())
                    mActive.remove(id);
                else
                    mHandler.postDelayed(this, code.timeLeft());
            }
        }, code.timeLeft());

        return code;
    }

    public Pair<String, String> getLabel(int position) {
        String uuid = mItems.get(position);
        Token token = Token.deserialize(mSharedPreferences.getString(uuid, null));

        Pair<String, String> label = new Pair<String, String>(token.getLabel(), token.getIssuer());
        return label;
    }
    public TokenIcon getTokenIcon(int position) {
        String uuid = mItems.get(position);
        Token token = Token.deserialize(mSharedPreferences.getString(uuid, null));

        return new TokenIcon(token, mContext);
    }

    public void setLabel(int position, String account, String issuer) throws IOException {
        String uuid = mItems.get(position);
        Token token = Token.deserialize(mSharedPreferences.getString(uuid, null));

        token.setIssuer(issuer);
        token.setLabel(account);

        // Save everything else.
        if (!storeItems().putString(uuid, token.serialize()).commit()) {
            throw new IOException();
        }

        // Save to Backup
        if (mTokenBackup.isProvisioned()) {
            try {
                mTokenBackup.save(uuid, token.serialize(), null, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onActivated(ViewHolder holder) {
    }

    @Override
    public void onShare(String code) {
    }
}
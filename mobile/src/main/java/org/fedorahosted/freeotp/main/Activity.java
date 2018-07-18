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

import android.Manifest;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.Html;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.main.share.ShareFragment;
import org.fedorahosted.freeotp.utils.GridLayoutItemDecoration;
import org.fedorahosted.freeotp.utils.SelectableAdapter;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.crypto.SecretKey;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class Activity extends AppCompatActivity
    implements SelectableAdapter.EventListener, View.OnClickListener, View.OnLongClickListener {
    private List<WeakReference<ViewHolder>> mViewHolders = new LinkedList<>();
    private int mLongClickCount = 0;

    private FloatingActionButton mFloatingActionButton;
    private RecyclerView mRecyclerView;
    private Adapter mTokenAdapter;
    private TextView mEmpty;
    private Menu mMenu;

    private final RecyclerView.AdapterDataObserver mAdapterDataObserver =
        new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                mEmpty.setVisibility(mTokenAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                onChanged();
            }
        };

    private void onActivate(ViewHolder vh) {
        try {
            vh.displayCode(mTokenAdapter.getCode(vh.getAdapterPosition()));
        } catch (UserNotAuthenticatedException e) {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            Intent i = km.createConfirmDeviceCredentialIntent(vh.getIssuer(), vh.getLabel());

            mViewHolders.add(new WeakReference<ViewHolder>(vh));
            startActivityForResult(i, mViewHolders.size() - 1);
        } catch (KeyPermanentlyInvalidatedException e) {
            try {
                mTokenAdapter.delete(vh.getAdapterPosition());
            } catch (GeneralSecurityException | IOException f) {
                f.printStackTrace();
            }

            new AlertDialog.Builder(this)
                .setTitle(R.string.main_invalidated_title)
                .setMessage(R.string.main_invalidated_message)
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        while (mViewHolders.size() > 0) {
            ViewHolder holder = mViewHolders.remove(requestCode).get();

            if (resultCode == Activity.RESULT_OK && holder != null)
                onActivate(holder);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Don't let other apps screenshot token codes...
        getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);

        mFloatingActionButton = findViewById(R.id.fab);
        mRecyclerView = findViewById(R.id.recycler);
        mEmpty = findViewById(android.R.id.empty);

        try {
            mTokenAdapter = new Adapter(getApplicationContext(), this) {
                @Override
                public void onActivated(ViewHolder holder) {
                    Activity.this.onActivate(holder);
                }

                @Override
                public void onShare(String code) {
                    Bundle b = new Bundle();
                    b.putString(ShareFragment.CODE_ID, code);

                    ShareFragment sf = new ShareFragment();
                    sf.setArguments(b);
                    sf.show(getSupportFragmentManager(), sf.getTag());
                }
            };
        } catch (GeneralSecurityException | IOException e) {
        }

        mFloatingActionButton.setOnClickListener(this);
        mFloatingActionButton.setOnLongClickListener(this);
        if (!ScanDialogFragment.hasCamera(getApplicationContext()))
            mFloatingActionButton.hide();

        int margin = getResources().getDimensionPixelSize(R.dimen.margin);
        mRecyclerView.setAdapter(mTokenAdapter);
        mRecyclerView.addItemDecoration(new GridLayoutItemDecoration(margin));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                /* Hide FAB on scroll-down, revealing the bottom token. */
                if (mFloatingActionButton.getVisibility() == View.VISIBLE) {
                    if (dy > 0)
                        mFloatingActionButton.hide();
                } else if (dy < 0 && ScanDialogFragment.hasCamera(getApplicationContext())) {
                    mFloatingActionButton.show();
                }
            }
        });

        mTokenAdapter.registerAdapterDataObserver(mAdapterDataObserver);
        mAdapterDataObserver.onChanged();

        onNewIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();

        int p = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT);
        if (p != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] { Manifest.permission.USE_FINGERPRINT }, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                try {
                    Resources r = getResources();
                    PackageManager pm = getPackageManager();
                    PackageInfo info = pm.getPackageInfo(getPackageName(), 0);

                    new AlertDialog.Builder(this)
                        .setTitle(r.getString(R.string.main_about_title,
                                info.versionName, info.versionCode))
                        .setMessage(Html.fromHtml(r.getString(R.string.main_about_message)))
                        .setPositiveButton(R.string.close, null)
                        .show();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;

            case R.id.action_down:
                if (mTokenAdapter.isSelected(mTokenAdapter.getItemCount() - 1))
                    return true;

                for (Integer i : new TreeSet<>(mTokenAdapter.getSelected().descendingSet()))
                    mTokenAdapter.move(i, i + 1);

                mRecyclerView.scrollToPosition(mTokenAdapter.getSelected().first());
                return true;

            case R.id.action_up:
                if (mTokenAdapter.isSelected(0))
                    return true;

                for (Integer i : new TreeSet<>(mTokenAdapter.getSelected()))
                    mTokenAdapter.move(i, i - 1);

                mRecyclerView.scrollToPosition(mTokenAdapter.getSelected().first());
                return true;

            case R.id.action_delete:
                new AlertDialog.Builder(this)
                    .setTitle(R.string.main_deletion_title)
                    .setMessage(R.string.main_deletion_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (Integer i : new TreeSet<>(mTokenAdapter.getSelected().descendingSet())) {
                                try { mTokenAdapter.delete(i); }
                                catch (GeneralSecurityException | IOException e) { }
                            }

                            mFloatingActionButton.show();
                        }
                    }).show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSelectEvent(NavigableSet<Integer> selected) {
        if (mMenu == null)
            return;

        for (int i = 0; i < mMenu.size(); i++) {
            MenuItem mi = mMenu.getItem(i);

            switch (mi.getItemId()) {
                case R.id.action_about:
                    mi.setVisible(selected.size() == 0);
                    break;

                case R.id.action_up:
                    mi.setVisible(selected.size() > 0);
                    mi.setEnabled(!mTokenAdapter.isSelected(0));
                    break;

                case R.id.action_down:
                    mi.setVisible(selected.size() > 0);
                    mi.setEnabled(!mTokenAdapter.isSelected(mTokenAdapter.getItemCount() - 1));
                    break;

                case R.id.action_delete:
                    mi.setVisible(selected.size() > 0);
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mLongClickCount++) {
            case 0:
                /* You have 15 seconds from the first click to enter random mode. */
                mFloatingActionButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mLongClickCount < 3)
                            mLongClickCount = 0;
                    }
                }, 15000);
                break;

            case 2:
                /* Random mode lasts for 15 seconds... */
                mFloatingActionButton.setImageResource(R.drawable.ic_add);
                mFloatingActionButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFloatingActionButton.setImageResource(R.drawable.ic_scan);
                        mLongClickCount = 0;
                    }
                }, 15000);
                break;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        if (mLongClickCount >= 3) {
            Pair<SecretKey, Token> pair = Token.random();
            addToken(pair.second.toUri(pair.first), true);
        } else {
            ScanDialogFragment scan = new ScanDialogFragment();
            scan.show(getSupportFragmentManager(), scan.getTag());
        }
    }

    void addToken(final Uri uri, boolean enableSecurity) {
        try {
            Pair<SecretKey, Token> pair = enableSecurity ? Token.parse(uri) : Token.parseUnsafe(uri);
            mRecyclerView.scrollToPosition(mTokenAdapter.add(pair.first, pair.second));
        } catch (Token.UnsafeUriException e) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.main_unsafe_title)
                .setMessage(R.string.main_unsafe_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add_anyway, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addToken(uri, false);
                    }
                }).show();
        } catch (Token.InvalidUriException e) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.main_invalid_title)
                .setMessage(R.string.main_invalid_message)
                .setPositiveButton(R.string.ok, null)
                .show();
        } catch (GeneralSecurityException | IOException e) {
            if (!e.getClass().equals(KeyStoreException.class) ||
                !e.getCause().getClass().equals(IllegalStateException.class)) {
                e.printStackTrace();
                new AlertDialog.Builder(this)
                        .setTitle(R.string.main_error_title)
                        .setMessage(R.string.main_error_message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return;
            }

            new AlertDialog.Builder(this)
                .setTitle(R.string.main_lock_title)
                .setMessage(R.string.main_lock_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.enable_lock_screen, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                            startActivity(intent);
                        }
                    })
                .show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null)
            addToken(uri, true);
    }
}
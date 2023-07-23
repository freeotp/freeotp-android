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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.fedorahosted.freeotp.Code;
import org.fedorahosted.freeotp.TokenIcon;
import org.fedorahosted.freeotp.ManualAdd;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class Activity extends AppCompatActivity
    implements SelectableAdapter.EventListener, View.OnClickListener, View.OnLongClickListener {
    private static final String LOGTAG = "Activity";

    private List<WeakReference<ViewHolder>> mViewHolders = new LinkedList<>();
    private int mLongClickCount = 0;

    private FloatingActionButton mFloatingActionButton;
    private FloatingActionButton mFABScan;
    private FloatingActionButton mFABManual;
    private RecyclerView mRecyclerView;
    private Adapter mTokenAdapter;
    private TextView mEmpty;
    private Menu mMenu;
    private MenuItem mAutoClipboard;
    ActivityResultLauncher<Intent> mManualAddLauncher;
    ActivityResultLauncher<Intent> mBackupSaveLauncher;
    ActivityResultLauncher<Intent> mRestoreSaveLauncher;
    private String mRestorePwd = "";
    private SharedPreferences mBackups;
    private TokenPersistence mTokenBackup;
    static final String BACKUP = "tokenBackup";
    static final String RESTORED = "restoreComplete";
    /* Generic settings preferences file */
    private SharedPreferences mSettings;
    static final String SETTINGS = "settings";
    static final String AUTO_COPY_CLIPBOARD = "copyClipboard";


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
            Log.i(LOGTAG, String.format("onActivate: adapter.getCode()"));
            Code code = mTokenAdapter.getCode(vh.getAdapterPosition());
            if (mSettings.getBoolean(AUTO_COPY_CLIPBOARD, false)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("code", code.getCode());
                clipboard.setPrimaryClip(clip);
            }

            Token.Type type = mTokenAdapter.getTokenType(vh.getAdapterPosition());
            Log.i(LOGTAG, String.format("onActivate: vh.displayCode()"));
            vh.displayCode(code, type);

        } catch (UserNotAuthenticatedException e) {
            Log.e(LOGTAG, "Exception", e);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            Intent i = km.createConfirmDeviceCredentialIntent(vh.getIssuer(), vh.getLabel());

            mViewHolders.add(new WeakReference<ViewHolder>(vh));
            startActivityForResult(i, mViewHolders.size() - 1);
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.e(LOGTAG, "Exception", e);
            try {
                mTokenAdapter.delete(vh.getAdapterPosition());
            } catch (GeneralSecurityException | IOException f) {
                Log.e(LOGTAG, "Exception", e);
            }

            new AlertDialog.Builder(this)
                .setTitle(R.string.main_invalidated_title)
                .setMessage(R.string.main_invalidated_message)
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        while (mViewHolders.size() > 0) {
            ViewHolder holder = mViewHolders.remove(requestCode).get();

            if (resultCode == Activity.RESULT_OK && holder != null)
                onActivate(holder);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg,Toast.LENGTH_SHORT).show();
    }

    interface LauncherCallback {
        void execute(Uri uri);
    }

    public ActivityResultLauncher<Intent> registerLauncher(LauncherCallback callback) {
        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        Uri uri = intent.getData();
                        if (uri != null) {
                            callback.execute(uri);
                        }
                    }
                });
    }

    private void initLaunchers() {
        try {
            mTokenBackup = new TokenPersistence(getApplicationContext());
        } catch (Exception e) {
            Log.e(LOGTAG, "Exception", e);
        }

        mManualAddLauncher = registerLauncher(uri -> addToken(uri, true));

        mBackupSaveLauncher = registerLauncher(uri -> {
            /* Copy tokenBackup to picked file on external storage */
            mTokenBackup.copyBackupToExternal(uri);
            showToast("Backup file saved to external storage");
        });

        mRestoreSaveLauncher = registerLauncher(uri -> {
            final EditText input = new EditText(getApplicationContext());
            input.setTypeface(Typeface.SERIF);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            /* Copy chosen file (externalBackup.xml) over apps tokenBackup.xml SP file */
            mTokenBackup.restoreBackupFromExternal(uri);

            /* Prompt for restore */
            showRestoreAlert(input);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Don't let other apps screenshot token codes...
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mBackups = getApplicationContext().getSharedPreferences(BACKUP, Context.MODE_PRIVATE);
        mSettings = getApplicationContext().getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);

        initLaunchers();

        mFABScan = findViewById(R.id.fab_scan);
        mFABManual = findViewById(R.id.fab_manual);
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
            Log.e(LOGTAG, "Exception", e);
        }

        mFloatingActionButton.setOnClickListener(this);
        mFloatingActionButton.setOnLongClickListener(this);
        if (!ScanDialogFragment.hasCamera(getApplicationContext()))
            mFloatingActionButton.hide();

        /* Scan QR code listener */
        mFABScan.setOnClickListener(view -> {
            mFABManual.hide();
            mFABScan.hide();
            ScanDialogFragment scan = new ScanDialogFragment();
            scan.show(getSupportFragmentManager(), scan.getTag());
        });

        /* Manual entry listener */
        mFABManual.setOnClickListener(view -> {
            mFABManual.hide();
            mFABScan.hide();
            Intent intent = new Intent(view.getContext(), ManualAdd.class);
            mManualAddLauncher.launch(intent);
        });


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

        /* EditTokenDialogFragment listener */
        getSupportFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, bundle) -> {
            String account = bundle.getString("account");
            String issuer = bundle.getString("issuer");

            int selected = mTokenAdapter.getSelected().first();

            try {
                mTokenAdapter.setLabel(selected, account, issuer);
                mTokenAdapter.notifyItemChanged(selected);
            } catch (IOException e) {
                Log.e(LOGTAG, "Exception", e);
            }
            // ensure viewholder in recyclerview is refreshed
        });

        if (mBackups.getBoolean(RESTORED, false)) {
            mBackups.edit().remove(RESTORED).apply();
            final EditText input = new EditText(this);
            input.setTypeface(Typeface.SERIF);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            showRestoreAlert(input);
        }

        onNewIntent(getIntent());


    }

    @Override
    protected void onStart() {
        super.onStart();

        int p = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT);
        if (p != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] { Manifest.permission.USE_FINGERPRINT }, 0);

        mBackups = getApplicationContext().getSharedPreferences(BACKUP, Context.MODE_PRIVATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        mAutoClipboard = menu.findItem(R.id.action_clipboard);
        /* Set checked/unchecked checkbox in menu for auto copy to clipboard setting */
        if(mSettings.getBoolean(AUTO_COPY_CLIPBOARD, false)) {
            mAutoClipboard.setIcon(R.drawable.ic_check_box_checked);
        } else {
            /* Should not be needed since this is the default, here as fallback */
            mAutoClipboard.setIcon(R.drawable.ic_check_box_blank);
        }
        return true;
    }

    private void showRestoreCancelAlert(final EditText input) {
        new AlertDialog.Builder(Activity.this)
                .setTitle(R.string.main_restore_cancel_title)
                .setMessage(R.string.main_restore_cancel_message)
                .setCancelable(false)
                .setNegativeButton(R.string.main_restore_go_back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getParent() != null) {
                            ((ViewGroup)input.getParent()).removeView(input); // <- fix
                        }
                        showRestoreAlert(input);
                    }
                })
                .setPositiveButton(R.string.main_restore_proceed, null)
                .show();
    }

    private void showRestoreAlert(final EditText input) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.main_restore_title)
                .setMessage(R.string.main_restore_message)
                .setCancelable(false)
                .setView(input)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showRestoreCancelAlert(input);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (input != null) {
                            mRestorePwd = input.getText().toString();

                            try {
                                mTokenAdapter.restoreTokens(mRestorePwd);
                            } catch (TokenPersistence.BadPasswordException e) {
                                Toast badpwd = Toast.makeText(getApplicationContext(),
                                        R.string.main_restore_bad_password,Toast.LENGTH_SHORT);
                                badpwd.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
                                badpwd.show();
                                dialog.dismiss();
                                if(input.getParent() != null) {
                                    ((ViewGroup)input.getParent()).removeView(input); // <- fix
                                }
                                input.setText("");
                                showRestoreAlert(input);
                            }
                        }
                    }
                }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                try {
                    Resources r = getResources();
                    PackageManager pm = getPackageManager();
                    PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
                    final SpannableString msg = new SpannableString(r.getString(R.string.main_about_message));
                    Linkify.addLinks(msg, Linkify.ALL);

                    final AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle(r.getString(R.string.main_about_title,
                                    info.versionName, info.versionCode))
                            .setMessage(Html.fromHtml(r.getString(R.string.main_about_message)))
                            .setPositiveButton(R.string.close, null)
                            .create();

                    dialog.show();

                    ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOGTAG, "Exception", e);
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

            case R.id.action_edit:
                int selected = mTokenAdapter.getSelected().first();
                TokenIcon token_icon = mTokenAdapter.getTokenIcon(selected);
                Pair<String, String> label = mTokenAdapter.getLabel(selected);
                Pair<Integer, String> image = token_icon.mImage;
                int color = token_icon.mColor;

                EditTokenDialogFragment edit = EditTokenDialogFragment.newInstance(label.first,
                        label.second, image.first, image.second, color);
                edit.show(getSupportFragmentManager(), edit.getTag());

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
                                    catch (GeneralSecurityException | IOException e) {
                                        Log.e(LOGTAG, "Exception", e);
                                    }
                                }

                                mFloatingActionButton.show();
                            }
                        }).show();

                return true;

            case R.id.action_clipboard:
                boolean copy_clipboard = mSettings.getBoolean(AUTO_COPY_CLIPBOARD, false);

                mSettings.edit().putBoolean(AUTO_COPY_CLIPBOARD, !copy_clipboard).apply();
                String s = String.format("Automatic copy-to-clipboard: %s", !copy_clipboard ? "Enabled" : "Disabled");
                Snackbar.make(findViewById(R.id.toolbar), s, Snackbar.LENGTH_SHORT)
                        .show();
                /* Update checkbox icon in menu */
                if(mAutoClipboard == null) return true;
                mAutoClipboard.setIcon(!copy_clipboard ? R.drawable.ic_check_box_checked : R.drawable.ic_check_box_blank);

                return true;

            case R.id.action_backup:

                Intent backupIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                backupIntent.addCategory(Intent.CATEGORY_OPENABLE);
                backupIntent.setType("application/xml");
                backupIntent.putExtra(Intent.EXTRA_TITLE, "externalBackup");
                mBackupSaveLauncher.launch(backupIntent);

                return true;

            case R.id.action_restore:

                Intent restoreIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                restoreIntent.addCategory(Intent.CATEGORY_OPENABLE);
                restoreIntent.setType("*/*");
                mRestoreSaveLauncher.launch(restoreIntent);

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
                case R.id.menu_todo:
                    mi.setVisible(selected.size() == 0);
                    break;
                case R.id.action_backup:
                case R.id.action_restore:
                case R.id.action_clipboard:

                case R.id.action_up:
                    mi.setVisible(selected.size() > 0);
                    mi.setEnabled(!mTokenAdapter.isSelected(0));
                    break;

                case R.id.action_down:
                    mi.setVisible(selected.size() > 0);
                    mi.setEnabled(!mTokenAdapter.isSelected(mTokenAdapter.getItemCount() - 1));
                    break;
                case R.id.action_edit:
                    mi.setVisible(selected.size() > 0);
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
            if (mFABManual.isShown()) {
                mFABManual.hide();
                mFABScan.hide();
            } else {
                mFABManual.show();
                mFABScan.show();
            }
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
            if (!e.getClass().equals(KeyStoreException.class)) {
                Log.e(LOGTAG, "Exception", e);
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
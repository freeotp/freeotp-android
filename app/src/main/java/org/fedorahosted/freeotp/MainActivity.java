/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
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

/*
 * Portions Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp;

import org.fedorahosted.freeotp.add.AddActivity;
import org.fedorahosted.freeotp.add.ScanActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.GridView;

import com.google.gson.Gson;

public class MainActivity extends Activity implements OnMenuItemClickListener {
    private TokenAdapter mTokenAdapter;
    private DataSetObserver mDataSetObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
        setContentView(R.layout.main);

        mTokenAdapter = new TokenAdapter(this);
        ((GridView) findViewById(R.id.grid)).setAdapter(mTokenAdapter);

        // Don't permit screenshots since these might contain OTP codes.
        getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);

        mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mTokenAdapter.getCount() == 0)
                    findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
                else
                    findViewById(android.R.id.empty).setVisibility(View.GONE);
            }
        };
        mTokenAdapter.registerDataSetObserver(mDataSetObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTokenAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTokenAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTokenAdapter.unregisterDataSetObserver(mDataSetObserver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_scan).setVisible(ScanActivity.haveCamera());
        menu.findItem(R.id.action_scan).setOnMenuItemClickListener(this);
        menu.findItem(R.id.action_add).setOnMenuItemClickListener(this);
        menu.findItem(R.id.action_about).setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_scan:
            startActivity(new Intent(this, ScanActivity.class));
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            return true;

        case R.id.action_add:
            startActivity(new Intent(this, AddActivity.class));
            return true;

        case R.id.action_about:
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        final Uri uri = intent.getData();
        if (uri != null) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_VIEW)) {
                TokenPersistence.addWithToast(this, uri.toString());
                return;
            }
            try {
                final Token token = new Token(uri);
                final String key = token.getID();
                final Intent out = new Intent();
                out.putExtra("key", key);
                String appname = intent.getStringExtra("appname");
                if (appname == null) {
                    appname = getString(R.string.default_appname);
                }
                final SharedPreferences prefs = getSharedPreferences(TokenPersistence.NAME, Context.MODE_PRIVATE);
                switch (action) {
                    case Intent.ACTION_GET_CONTENT:
                        if (prefs.contains(key)) {
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.attention)
                                    .setMessage(appname + getString(R.string.request_code) + "\"" + key + "\"")
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            out.putExtra("currentCode", new Gson().fromJson(prefs.getString(key, null), Token.class).generateCodes().getCurrentCode());
                                            setResult(Activity.RESULT_OK, out);
                                            finish();
                                        }
                                    })
                                    .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            setResult(Activity.RESULT_CANCELED, out);
                                            finish();
                                        }
                                    })
                                    .show()
                            ;
                        } else {
                            setResult(Activity.RESULT_CANCELED, out);
                            finish();
                        }
                        break;
                    case Intent.ACTION_INSERT:
                        if (prefs.contains(key)) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(R.string.error)
                                    .setMessage(String.format(getString(R.string.token_already_exists), key))
                                    .setCancelable(false)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            setResult(Activity.RESULT_CANCELED, out);
                                            finish();
                                        }
                                    })
                                    .show()
                            ;
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.attention)
                                    .setMessage(appname + getString(R.string.request_install_token) + "\"" + key + "\"")
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            try {
                                                new TokenPersistence(MainActivity.this).add(token);
                                                out.putExtra("currentCode", token.generateCodes().getCurrentCode());
                                                out.putExtra("secret", uri.getQueryParameter("secret"));
                                                setResult(Activity.RESULT_OK, out);
                                                finish();
                                            } catch (Token.TokenUriInvalidException e) {
                                                new AlertDialog.Builder(MainActivity.this)
                                                        .setTitle(R.string.error)
                                                        .setMessage(getString(R.string.bad_token_uri) + uri.toString())
                                                        .setCancelable(false)
                                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                setResult(Activity.RESULT_CANCELED, out);
                                                                finish();
                                                            }
                                                        })
                                                        .show()
                                                ;
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            setResult(Activity.RESULT_CANCELED, out);
                                            finish();
                                        }
                                    })
                                    .show()
                            ;
                        }
                        break;
                    default:
                        Log.e("LOG", "bad action: " + action);
                }
            } catch (Token.TokenUriInvalidException e) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.error)
                        .setMessage(getString(R.string.bad_token_uri) + uri.toString())
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .show()
                ;
            }
        }
    }
}

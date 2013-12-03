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

import java.util.Arrays;
import java.util.List;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;
import org.fedorahosted.freeotp.adapters.TokenAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
	private static final List<String> PROVIDERS = Arrays.asList(new String[] {
		"com.google.zxing.client.android", // Barcode Scanner
		"com.srowen.bs.android",           // Barcode Scanner+
		"com.srowen.bs.android.simple",    // Barcode Scanner+ Simple
		"com.google.android.apps.unveil"   // Google Goggles
	});

	private TokenAdapter ta;

	private String findAppPackage(Intent i) {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> ril = pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
		if (ril != null) {
			for (ResolveInfo ri : ril) {
				if (PROVIDERS.contains(ri.activityInfo.packageName))
					return ri.activityInfo.packageName;
			}
		}

		return null;
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ta = new TokenAdapter(this);
		((GridView) findViewById(R.id.grid)).setAdapter(ta);

		DataSetObserver dso = new DataSetObserver() {
			@Override
			public void onChanged() {
				super.onChanged();
				if (ta.getCount() == 0)
					findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
				else
					findViewById(android.R.id.empty).setVisibility(View.GONE);
			}
		};
		ta.registerDataSetObserver(dso);
		dso.onChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_add).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog ad = new AddTokenDialog(MainActivity.this) {
					@Override
					public void addToken(String uri) {
						try {
							ta.add(uri);
						} catch (TokenUriInvalidException e) {
							Toast.makeText(MainActivity.this, R.string.invalid_token, Toast.LENGTH_SHORT).show();
							e.printStackTrace();
						}
					}
				};

				ad.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.scan_qr_code), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent(ACTION_SCAN);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						i.addCategory(Intent.CATEGORY_DEFAULT);
						i.putExtra("SCAN_MODE", "QR_CODE_MODE");
						i.putExtra("SAVE_HISTORY", false);

						String pkg = findAppPackage(i);
						if (pkg != null) {
							i.setPackage(pkg);
							startActivityForResult(i, 0);
							return;
						}

						new AlertDialog.Builder(MainActivity.this)
							.setTitle(R.string.install_title)
							.setMessage(R.string.install_message)
							.setPositiveButton(R.string.yes, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									Uri uri = Uri.parse("market://details?id=" + PROVIDERS.get(0));
									Intent intent = new Intent(Intent.ACTION_VIEW, uri);
									try {
										startActivity(intent);
									} catch (ActivityNotFoundException e) {
										e.printStackTrace();
									}
								}
							})
							.setNegativeButton(R.string.no, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									return;
								}
							})
							.create().show();
					}
				});

				ad.show();

		        return true;
			}
		});

		menu.findItem(R.id.action_about).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				new AboutDialog(MainActivity.this).show();
				return true;
			}
		});

        return true;
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			try {
				ta.add(intent.getStringExtra("SCAN_RESULT"));
			} catch (TokenUriInvalidException e) {
				Toast.makeText(this, R.string.invalid_token, Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		}
	}
}

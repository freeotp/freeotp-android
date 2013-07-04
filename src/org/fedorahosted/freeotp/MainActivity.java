/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 * see file 'COPYING' for use and warranty information
 *
 * This program is free software you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;

import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

public class MainActivity extends ListActivity {
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
        setListAdapter(ta);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        
        menu.findItem(R.id.action_add).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
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
					return false;
				}

				new AlertDialog.Builder(MainActivity.this)
					.setTitle(R.string.install_title)
					.setMessage(R.string.install_message)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
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
					.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialogInterface, int i) {
							return;
						}
					})
					.create().show();

		        return false;
			}
		});

        return true;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            try {
				ta.add(this, intent.getStringExtra("SCAN_RESULT"));
			} catch (NoSuchAlgorithmException e) {
				Toast.makeText(this, R.string.token_scan_invalid, Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			} catch (TokenUriInvalidException e) {
				Toast.makeText(this, R.string.token_scan_invalid, Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
        }
    }
}

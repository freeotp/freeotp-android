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

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;
import org.fedorahosted.freeotp.adapters.TokenAdapter;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnMenuItemClickListener {
	private TokenAdapter mTokenAdapter;
	private DataSetObserver mDataSetObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mTokenAdapter = new TokenAdapter(this);
		((GridView) findViewById(R.id.grid)).setAdapter(mTokenAdapter);

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
		mDataSetObserver.onChanged();
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTokenAdapter.unregisterDataSetObserver(mDataSetObserver);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_add).setOnMenuItemClickListener(this);
		menu.findItem(R.id.action_about).setOnMenuItemClickListener(this);
        return true;
    }

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			// If the device has a camera available, try to scan for QR code
			PackageManager pm = getPackageManager();
			if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) &&
				pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
				new CameraDialogFragment().show(getFragmentManager(),
						CameraDialogFragment.FRAGMENT_TAG);
			} else {
				new ManualDialogFragment().show(getFragmentManager(),
						ManualDialogFragment.FRAGMENT_TAG);
			}

			return true;

		case R.id.action_about:
			new AboutDialogFragment().show(getFragmentManager(),
					AboutDialogFragment.FRAGMENT_TAG);
			return true;
		}

		return false;
	}

	public void tokenURIReceived(String uri) {
		try {
			mTokenAdapter.add(uri);
		} catch (TokenUriInvalidException e) {
			Toast.makeText(this, R.string.invalid_token, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}
}

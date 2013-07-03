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

package org.fedorahosted.freeotp;

import java.security.NoSuchAlgorithmException;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;

import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

public class MainActivity extends ListActivity {
	private TokenAdapter ta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ta = new TokenAdapter(this);
        setListAdapter(ta);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        
        menu.findItem(R.id.action_add).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent i = new Intent("com.google.zxing.client.android.SCAN");
		        i.putExtra("SCAN_MODE", "QR_CODE_MODE");
		        i.putExtra("SAVE_HISTORY", false);
		        startActivityForResult(i, 0);
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

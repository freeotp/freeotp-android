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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TokenAdapter extends BaseAdapter {
	private static class Ticker extends Handler {
		private static interface OnTickListener {
			public void tick(ProgressBar pb);
		}
		
		private Map<ProgressBar, OnTickListener> map = new HashMap<ProgressBar, OnTickListener>();
		
		@Override
		public void handleMessage(Message msg) {
			for (ProgressBar pb : map.keySet())
				map.get(pb).tick(pb);
			
			sendEmptyMessageDelayed(0, 200);
		}
		
		public void set(ProgressBar pb, OnTickListener otl) {
			map.put(pb, otl);
		}
	}
	
	private List<Token> tokens = new ArrayList<Token>();
	private Ticker ticker = new Ticker();
	
	private void sort() {
		Collections.sort(tokens, new Comparator<Token>() {
			public int compare(Token lhs, Token rhs) {
				return lhs.getTitle().compareTo(rhs.getTitle());
			}
		});
	}
	
	public TokenAdapter(Context ctx) {
		tokens.addAll(Token.getTokens(ctx));
		ticker.sendEmptyMessageDelayed(0, 200);
		sort();
	}
	
	@Override
	public int getCount() {
		return tokens.size();
	}

	@Override
	public Token getItem(int position) {
		return tokens.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Context ctx = parent.getContext();
		
		if (convertView == null) {
			switch (getItem(position).getType()) {
			case HOTP:
				convertView = View.inflate(ctx, R.layout.hotp, null);
				break;
				
			case TOTP:
				convertView = View.inflate(ctx, R.layout.totp, null);
				break;
			}
		}
		
		final Token item = getItem(position);
		final TextView code = (TextView) convertView.findViewById(R.id.code);
		final TextView title = (TextView) convertView.findViewById(R.id.title);
		final ImageButton ib = (ImageButton) convertView.findViewById(R.id.button);
		
		code.setText(item.getCurrentTokenValue(ctx, false));
		title.setText(item.getTitle());
		
		ib.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String delmsg = ctx.getString(R.string.delete_message);

				AlertDialog ad = new AlertDialog.Builder(ctx)
				.setTitle("Delete")
				.setMessage(delmsg + item.getTitle())
				.setIcon(android.R.drawable.ic_delete)
				.setPositiveButton(R.string.delete,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								tokens.remove(tokens.indexOf(item));
								item.remove(ctx);
								notifyDataSetChanged();
								dialog.dismiss();
							}
	
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						}).create();
				ad.show();
			}
		});

		switch (getItem(position).getType()) {
		case HOTP:
			ImageButton hotp = (ImageButton) convertView.findViewById(R.id.hotpButton);
			hotp.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					code.setText(item.getCurrentTokenValue(ctx, true));
				}
			});
			break;
			
		case TOTP:
			ProgressBar pb = (ProgressBar) convertView.findViewById(R.id.totpProgressBar);
			ticker.set(pb, new Ticker.OnTickListener() {
				public void tick(ProgressBar pb) {
					int max = pb.getMax();
					int pro = item.getProgress();
					pb.setProgress(max - pro);
					if (pro < max / 20 || pro > max / 20 * 19)
						code.setText(item.getCurrentTokenValue(ctx, false));
				}
			});
			break;
		}
		
		return convertView;
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		switch (getItem(position).getType()) {
		case HOTP:
			return 0;
		case TOTP:
			return 1;
		default:
			return -1;
		}
	}
	
	public void add(Context ctx, String uri) throws NoSuchAlgorithmException, TokenUriInvalidException {
		Token t = new Token(uri);
		t.save(ctx);
		tokens.add(t);
		sort();
		notifyDataSetChanged();
	}
}

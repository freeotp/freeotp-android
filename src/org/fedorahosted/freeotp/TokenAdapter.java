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

package org.fedorahosted.freeotp;

import java.util.HashMap;
import java.util.Map;

import org.fedorahosted.freeotp.Token.TokenUriInvalidException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
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

		private final Map<ProgressBar, OnTickListener> map = new HashMap<ProgressBar, OnTickListener>();

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

	private static class ViewHolder {
		int index;
		Token token;
		TextView code;
		TextView title;
	}

	private final TokenStore ts;
	private final Ticker ticker = new Ticker();

	public TokenAdapter(Context ctx) {
		ts = new TokenStore(ctx);
		ticker.sendEmptyMessageDelayed(0, 200);
	}

	@Override
	public int getCount() {
		return ts.getTokenCount();
	}

	@Override
	public Token getItem(int position) {
		return ts.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Token token = ts.get(position);

        if (convertView == null)
            convertView = newView(parent.getContext(), token.getType(), parent);

        bindView(parent.getContext(), convertView, token, position);
        return convertView;
	}

	public void bindView(Context ctx, View view, Token token, int position) {
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.index = position;
		holder.token = token;

		holder.code.setText(token.getPlaceholder());
		holder.title.setText(token.getIssuer() + ": " + token.getLabel());
	}

	public View newView(Context ctx, Token.TokenType type, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewHolder holder = new ViewHolder();

		View view = null;
		switch (type) {
		case HOTP:
			view = inflater.inflate(R.layout.hotp, parent, false);
			ImageButton hotp = (ImageButton) view.findViewById(R.id.hotpButton);
			hotp.setTag(holder);
			hotp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ViewHolder holder = (ViewHolder) v.getTag();
					holder.code.setText(holder.token.getCode());
					holder.token.increment();
					ts.save(holder.token);
				}
			});
			break;

		case TOTP:
			view = inflater.inflate(R.layout.totp, parent, false);
			ProgressBar pb = (ProgressBar) view.findViewById(R.id.totpProgressBar);
			pb.setTag(holder);
			ticker.set(pb, new Ticker.OnTickListener() {
				@Override
				public void tick(ProgressBar pb) {
					ViewHolder holder = (ViewHolder) pb.getTag();
					pb.setProgress(pb.getMax() - holder.token.getProgress());
					holder.code.setText(holder.token.getCode());
				}
			});
			break;
		}

		ImageButton ib = (ImageButton) view.findViewById(R.id.button);
		ib.setTag(holder);
		ib.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final ViewHolder holder = (ViewHolder) v.getTag();

				StringBuilder sb = new StringBuilder();
				sb.append(v.getContext().getString(R.string.delete_message));
				sb.append(holder.token.getIssuer());
				sb.append("\n");
				sb.append(holder.token.getLabel());

				AlertDialog ad = new AlertDialog.Builder(v.getContext())
					.setTitle("Delete")
					.setMessage(sb.toString())
					.setIcon(android.R.drawable.ic_delete)
					.setPositiveButton(R.string.delete,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									ts.del(holder.index);
									notifyDataSetChanged();
									dialog.dismiss();
								}

							})
					.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							})
					.create();
				ad.show();
			}
		});

		holder.code = (TextView) view.findViewById(R.id.code);
		holder.title = (TextView) view.findViewById(R.id.title);
		view.setTag(holder);

		return view;
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
			return IGNORE_ITEM_VIEW_TYPE;
		}
	}

	public void add(Context ctx, String uri) throws TokenUriInvalidException {
		ts.add(new Token(uri));
		notifyDataSetChanged();
	}
}

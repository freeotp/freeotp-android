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

package org.fedorahosted.freeotp.adapters;

import java.lang.ref.WeakReference;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.Token.TokenType;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;


public class TokenAdapter extends TokenPersistenceBaseAdapter {
	private class ViewHolder {
		Token token;
		TextView code;
		TextView label;
		TextView issuer;
		ProgressBar progress;
	}

	private static class Ticker extends Handler {
		WeakReference<View> wr;

		public Ticker(View view) {
			wr = new WeakReference<View>(view);
		}

		@Override
		public void handleMessage(Message msg) {
			View view = wr.get();
			if (view == null)
				return;

			ViewHolder holder = (ViewHolder) view.getTag();
			int progress = 1000 - holder.token.getProgress();
			holder.code.setText(holder.token.getCode());
			holder.progress.setProgress(progress);
			if (progress > 0 && progress < 950)
				view.setEnabled(true);
			start();
		}

		public void start() {
			stop();
			sendEmptyMessageDelayed(0, 100);
		}

		public void stop() {
			removeMessages(0);
		}
	}

	private final LayoutInflater mLayoutInflater;

	public TokenAdapter(Context ctx) {
		super(ctx);
		mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	protected void bindView(View view, int position) {
		super.bindView(view, position);

		ViewHolder holder = (ViewHolder) view.getTag();
		holder.token = getItem(position);

		// Update views
		holder.code.setText(holder.token.getCode());
		holder.label.setText(holder.token.getLabel());
		holder.issuer.setText(holder.token.getIssuer());
		holder.progress.setProgress(holder.token.getProgress());

		// Update click listener
		View.OnClickListener ocl = null;
		if (holder.token.getType() == TokenType.HOTP) {
			ocl = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ViewHolder holder = (ViewHolder) v.getTag();
					holder.token.increment();
					holder.code.setText(holder.token.getCode());
					save(holder.token);
					v.setEnabled(false);
				}
			};
		}
		view.setOnClickListener(ocl);

		if (holder.token.getType() == TokenType.TOTP)
			view.setBackgroundResource(R.drawable.token_normal);
		else
			view.setBackgroundResource(R.drawable.token);
	}

	@Override
	protected View createView(ViewGroup parent, int type) {
		View view = mLayoutInflater.inflate(R.layout.token, parent, false);

		ViewHolder holder = new ViewHolder();
		holder.code = (TextView) view.findViewById(R.id.code);
		holder.label = (TextView) view.findViewById(R.id.label);
		holder.issuer = (TextView) view.findViewById(R.id.issuer);
		holder.progress = (ProgressBar) view.findViewById(R.id.progress);
		view.setTag(holder);

		new Ticker(view).start();

		return view;
	}

	@Override
	protected CompoundButton getCompoundButton(View view) {
		return (CompoundButton) view.findViewById(R.id.checkBox);
	}
}

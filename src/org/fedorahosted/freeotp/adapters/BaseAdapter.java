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

import android.view.View;
import android.view.ViewGroup;

public abstract class BaseAdapter extends android.widget.BaseAdapter {
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			int type = getItemViewType(position);
			convertView = createView(parent, type);
			processView(convertView, type);
		}

		bindView(convertView, position);
		return convertView;
	}

	protected abstract void bindView(View view, int position);
	protected abstract void processView(View view, int type);
	protected abstract View createView(ViewGroup parent, int type);
}

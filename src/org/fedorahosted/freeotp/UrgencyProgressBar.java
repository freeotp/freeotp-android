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

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class UrgencyProgressBar extends ProgressBar {
	public UrgencyProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public UrgencyProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UrgencyProgressBar(Context context) {
		super(context);
	}

	@Override
	public synchronized void setProgress(int progress) {
		super.setProgress(progress);

		int percent = progress * 100 / getMax();
		if (percent > 33 || progress == 0)
			getProgressDrawable().clearColorFilter();
		else {
			int green = 0xe0 * percent / 33;
			getProgressDrawable().setColorFilter(Color.RED | (green << 8), Mode.SRC_IN);
		}
	}


}

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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class CircleProgressBar extends ProgressBar {
	private Paint paint;
	private RectF rectf;
	private Rect rect;

	public CircleProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup();
	}

	public CircleProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	public CircleProgressBar(Context context) {
		super(context);
		setup();
	}

	private void setup() {
		paint = new Paint();
        rectf = new RectF();
        rect = new Rect();

        paint.setColor(0x33333300);
        paint.setAlpha(0x99);
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL_AND_STROKE);
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		getDrawingRect(rect);
		rect.left += getPaddingLeft() + 2;
		rect.top += getPaddingTop() + 2;
		rect.right -= getPaddingRight() + 2;
		rect.bottom -= getPaddingBottom() + 2;

		rectf.set(rect);
		canvas.drawArc(rectf, -90, getProgress() * 360 / getMax(), true, paint);
	}
}

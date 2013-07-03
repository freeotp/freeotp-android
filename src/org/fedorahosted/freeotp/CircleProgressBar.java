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

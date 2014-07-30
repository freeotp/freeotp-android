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
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ProgressCircle extends View {
    private Paint   mPaintInner;
    private Paint   mPaintOuter;
    private RectF   mRectF;
    private Rect    mRect;
    private int     mProgressInner;
    private int     mProgressOuter;
    private int     mMax;
    private boolean mOuter;

    public ProgressCircle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context, attrs);
    }

    public ProgressCircle(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    public ProgressCircle(Context context) {
        super(context);
        setup(context, null);
    }

    private void setup(Context context, AttributeSet attrs) {
        if (attrs != null) {
            Theme t = context.getTheme();
            TypedArray a = t.obtainStyledAttributes(attrs, R.styleable.ProgressCircle, 0, 0);

            try {
                mMax = a.getInteger(R.styleable.ProgressCircle_max, 100);
                mOuter = a.getBoolean(R.styleable.ProgressCircle_outer, false);
            } finally {
                a.recycle();
            }
        }

        mRectF = new RectF();
        mRect = new Rect();

        mPaintInner = new Paint();
        mPaintInner.setARGB(0x99, 0x33, 0x33, 0x33);
        mPaintInner.setAntiAlias(true);
        mPaintInner.setStyle(Style.FILL_AND_STROKE);
        mPaintInner.setStrokeCap(Paint.Cap.BUTT);

        mPaintOuter = new Paint();
        mPaintOuter.setARGB(0x99, 0x33, 0x33, 0x33);
        mPaintOuter.setAntiAlias(true);
        mPaintOuter.setStyle(Style.STROKE);
        mPaintOuter.setStrokeCap(Paint.Cap.BUTT);
        mPaintOuter.setStrokeWidth(8);
    }

    public void setOuter(boolean outer) {
        this.mOuter = outer;
    }

    public boolean getOuter() {
        return mOuter;
    }

    public void setMax(int max) {
        this.mMax = max;
    }

    public int getMax() {
        return mMax;
    }

    public void setProgress(int inner) {
        mProgressInner = inner;

        int percent = mProgressInner * 100 / getMax();
        if (percent > 25 || mProgressInner == 0)
            mPaintInner.setARGB(0x99, 0x33, 0x33, 0x33);
        else
            mPaintInner.setARGB(0x99, 0xff, 0xe0 * percent / 25, 0x00);

        invalidate();
    }

    public void setProgress(int inner, int outer) {
        mProgressOuter = outer;
        setProgress(inner);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        getDrawingRect(mRect);

        mRect.left += getPaddingLeft() + 2;
        mRect.top += getPaddingTop() + 2;
        mRect.right -= getPaddingRight() + 2;
        mRect.bottom -= getPaddingBottom() + 2;
        mRectF.set(mRect);
        if (mOuter) {
            canvas.drawArc(mRectF, -90, mProgressOuter * 360 / getMax(), false, mPaintOuter);

            mRect.left += 8;
            mRect.top += 8;
            mRect.right -= 8;
            mRect.bottom -= 8;
            mRectF.set(mRect);
        }
        canvas.drawArc(mRectF, -90, mProgressInner * 360 / getMax(), true, mPaintInner);
    }
}

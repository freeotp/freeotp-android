/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2014  Nathaniel McCallum, Red Hat
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
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

public class CardImageView extends ImageView {
    private static final int ROUNDING_DP = 2;
    private float mRadius = 0;
    private RectF mRect = new RectF();
    private Path mPath = new Path();

    private void setup() {
        // Canvas.clipPath() on hardware requires v18+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Convert DP to pixels.
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ROUNDING_DP, dm);
    }

    public CardImageView(Context context) {
        super(context);
        setup();
    }

    public CardImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public CardImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Create the rectangle.
        mRect.set(0, 0, this.getWidth() + mRadius, this.getHeight());

        // Create the clip path.
        mPath.reset();
        mPath.addRoundRect(mRect, mRadius, mRadius, Path.Direction.CW);

        // Clip and draw.
        canvas.clipPath(mPath);
        super.onDraw(canvas);
    }
}

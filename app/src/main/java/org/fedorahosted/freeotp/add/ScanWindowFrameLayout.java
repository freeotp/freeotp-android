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

package org.fedorahosted.freeotp.add;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ScanWindowFrameLayout extends FrameLayout {
    public ScanWindowFrameLayout(Context context) {
        super(context);
    }

    public ScanWindowFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScanWindowFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Ensure that this view is always a square.
        if (widthMeasureSpec > heightMeasureSpec)
            widthMeasureSpec = heightMeasureSpec;
        else
            heightMeasureSpec = widthMeasureSpec;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}

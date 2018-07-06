/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
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

package org.fedorahosted.freeotp.main.share;

import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class SortableItem<T extends SortableItem<T>> implements Comparable<T> {
    interface OnChangeListener<T extends SortableItem<T>> {
        void onChange(SortableItem<T> item);
    }

    private Set<OnChangeListener<T>> mOnChangeListeners =
            Collections.synchronizedSet(new HashSet<OnChangeListener<T>>());
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public void removeOnChangeListener(OnChangeListener<T> onChangeListener) {
        mOnChangeListeners.remove(onChangeListener);
    }

    public void addOnChangeListener(OnChangeListener<T> onChangeListener) {
        mOnChangeListeners.add(onChangeListener);
    }

    public void notifyOnChangeListeners() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (OnChangeListener<T> onChangeListener : mOnChangeListeners)
                    onChangeListener.onChange(SortableItem.this);
            }
        });
    }
}

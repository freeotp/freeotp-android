package org.fedorahosted.freeotp.util;

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

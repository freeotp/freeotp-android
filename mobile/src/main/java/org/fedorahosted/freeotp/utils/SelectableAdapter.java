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

package org.fedorahosted.freeotp.utils;

import android.support.v7.widget.RecyclerView;

import java.util.NavigableSet;
import java.util.TreeSet;

public abstract class SelectableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    public interface EventListener {
        void onSelectEvent(NavigableSet<Integer> selected);
    }

    private NavigableSet<Integer> mSelected = new TreeSet<>();
    private EventListener mListener;

    private RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mSelected.clear();
                mListener.onSelectEvent(mSelected);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                for (Integer i : new TreeSet<>(mSelected.descendingSet())) {
                    if (i < positionStart)
                        break;

                    mSelected.remove(i);
                    mSelected.add(i + itemCount);
                }

                mListener.onSelectEvent(mSelected);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);

                for (Integer i : new TreeSet<>(mSelected)) {
                    if (i < positionStart)
                        continue;

                    mSelected.remove(i);
                    if (i > positionStart + itemCount)
                        mSelected.add(i - itemCount);
                }

                mListener.onSelectEvent(mSelected);
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);

                NavigableSet<Integer> selected = new TreeSet<>();
                for (Integer i : mSelected) {
                    int j;

                    /* Before all affected items: no change. */
                    if (i < fromPosition && i < toPosition)
                        j = i;

                    /* After all affected items: no change. */
                    else if (i >= fromPosition + itemCount && i >= toPosition + itemCount)
                        j = i;

                    /* In the old position: move to new position. */
                    else if (i >= fromPosition && i < fromPosition + itemCount)
                        j = i + toPosition - fromPosition;

                    /* Shift upward. */
                    else if (fromPosition < toPosition)
                        j = i - itemCount;

                    /* Shift downward.*/
                    else
                        j = i + itemCount;

                    selected.add(j);
                }

                mListener.onSelectEvent(mSelected = selected);
            }
        };

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        unregisterAdapterDataObserver(mObserver);
        super.setHasStableIds(hasStableIds);
        registerAdapterDataObserver(mObserver);
    }

    public SelectableAdapter(EventListener listener) {
        super();
        mListener = listener;
        registerAdapterDataObserver(mObserver);
    }

    public NavigableSet<Integer> getSelected() {
        return mSelected;
    }

    public void setSelected(int position, boolean selected) {
        boolean change;

        if (selected)
            change = mSelected.add(position);
        else
            change = mSelected.remove(position);

        if (change)
            mListener.onSelectEvent(mSelected);
    }

    public boolean isSelected(int position) {
        return mSelected.contains(position);
    }
}

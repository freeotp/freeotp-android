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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public abstract class SortableItemAdapter<T extends SortableItem<T>, VH extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<VH> implements SortableItem.OnChangeListener<T> {
    private List<T> mItems = new ArrayList<>();

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public T get(int pos) {
        return mItems.get(pos);
    }

    public void add(T item) {
        item.addOnChangeListener(this);

        int pos = Collections.binarySearch(mItems, item);
        if (pos < 0)
            pos += mItems.size() + 1;

        notifyItemInserted(pos);
        mItems.add(pos, item);
    }

    public boolean remove(T item) {
        int pos = Collections.binarySearch(mItems, item);
        if (pos < 0)
            return false;

        item.removeOnChangeListener(this);
        notifyItemRemoved(pos);
        mItems.remove(pos);
        return true;
    }

    @Override
    public void onChange(SortableItem<T> item) {
        Collections.sort(mItems);
        notifyDataSetChanged();
    }
}

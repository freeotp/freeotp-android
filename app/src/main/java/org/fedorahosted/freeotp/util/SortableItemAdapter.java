package org.fedorahosted.freeotp.util;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

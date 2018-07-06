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

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.fedorahosted.freeotp.R;

class Adapter extends SortableItemAdapter<Adapter.Item, Adapter.ViewHolder> {
    static class Item extends SortableItem<Item> {
        public interface OnClickListener {
            void onClick(Item item);
        }

        private OnClickListener mOnClickListener;
        private boolean mEnabled = true;
        private int mPriority = 0;
        private String mSubtitle;
        private String mTitle;
        private int mImage;

        synchronized void setOnClickListener(OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
            notifyOnChangeListeners();
        }

        synchronized String getSubtitle() {
            return mSubtitle;
        }

        synchronized void setSubtitle(String subtitle) {
            mSubtitle = subtitle;
            notifyOnChangeListeners();
        }

        synchronized String getTitle() {
            return mTitle;
        }

        synchronized void setTitle(String title) {
            mTitle = title;
            notifyOnChangeListeners();
        }

        synchronized boolean getEnabled() {
            return mEnabled;
        }

        synchronized void setEnabled(boolean enabled) {
            mEnabled = enabled;
            notifyOnChangeListeners();
        }

        synchronized int getPriority() {
            return mPriority;
        }

        synchronized void setPriority(int priority) {
            mPriority = priority;
            notifyOnChangeListeners();
        }

        synchronized int getImage() {
            return mImage;
        }

        synchronized void setImage(int image) {
            mImage = image;
            notifyOnChangeListeners();
        }

        @Override
        public int compareTo(@NonNull Item item) {
            int type = getPriority() - item.getPriority();
            if (type != 0)
                return type;

            if (getTitle() == null || item.getTitle() == null)
                return 0;

            int title = getTitle().compareTo(item.getTitle());
            if (title != 0)
                return type;

            if (getSubtitle() == null || item.getSubtitle() == null)
                return 0;

            return getSubtitle().compareTo(item.getSubtitle());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ProgressBar mProgress;
        TextView mSubtitle;
        ImageView mImage;
        TextView mTitle;
        View mRow;

        ViewHolder(View itemView) {
            super(itemView);
            mRow = itemView;
            mImage = itemView.findViewById(R.id.image);
            mTitle = itemView.findViewById(R.id.title);
            mSubtitle = itemView.findViewById(R.id.subtitle);
            mProgress = itemView.findViewById(R.id.progress);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater li = LayoutInflater.from(parent.getContext());
        return new ViewHolder(li.inflate(R.layout.target, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Item item = get(position);

        holder.mProgress.setVisibility(item.mOnClickListener == null ? View.VISIBLE : View.GONE);
        holder.mImage.setVisibility(item.mOnClickListener != null ? View.VISIBLE : View.GONE);
        holder.mSubtitle.setText(item.getSubtitle());
        holder.mTitle.setText(item.getTitle());

        holder.mRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item.mOnClickListener.onClick(item);
            }
        });

        if (item.mOnClickListener != null)
            holder.mImage.setImageResource(item.getImage());

        holder.mProgress.setEnabled(item.mOnClickListener != null && item.getEnabled());
        holder.mSubtitle.setEnabled(item.mOnClickListener != null && item.getEnabled());
        holder.mImage.setEnabled(item.mOnClickListener != null && item.getEnabled());
        holder.mTitle.setEnabled(item.mOnClickListener != null && item.getEnabled());
        holder.mRow.setEnabled(item.mOnClickListener != null && item.getEnabled());
    }
}

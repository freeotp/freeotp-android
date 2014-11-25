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

import android.content.ClipData;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class BaseReorderableAdapter extends BaseAdapter {
    private class Reference<T> {
        public Reference(T t) {
            reference = t;
        }

        T reference;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            int type = getItemViewType(position);
            convertView = createView(parent, type);

            convertView.setOnDragListener(new OnDragListener() {
                @Override
                public boolean onDrag(View dstView, DragEvent event) {
                    Reference<View> ref = (Reference<View>) event.getLocalState();
                    final View srcView = ref.reference;

                    switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        srcView.setVisibility(View.VISIBLE);
                        dstView.setVisibility(View.INVISIBLE);

                        move(((Integer) srcView.getTag(R.id.reorder_key)).intValue(),
                             ((Integer) dstView.getTag(R.id.reorder_key)).intValue());
                        ref.reference = dstView;
                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        srcView.post(new Runnable() {
                            @Override
                            public void run() {
                                srcView.setVisibility(View.VISIBLE);
                            }
                        });
                        break;
                    }

                    return true;
                }
            });

            convertView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(final View view) {
                    // Force a reset of any states
                    notifyDataSetChanged();

                    // Start the drag on the main loop to allow
                    // the above state reset to settle.
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            ClipData data = ClipData.newPlainText("", "");
                            DragShadowBuilder sb = new View.DragShadowBuilder(view);
                            view.startDrag(data, sb, new Reference<View>(view), 0);
                        }
                    });

                    return true;
                }
            });
        }

        convertView.setTag(R.id.reorder_key, Integer.valueOf(position));
        bindView(convertView, position);
        return convertView;
    }

    protected abstract void move(int fromPosition, int toPosition);

    protected abstract void bindView(View view, int position);

    protected abstract View createView(ViewGroup parent, int type);
}

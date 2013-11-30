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

package org.fedorahosted.freeotp.adapters;

import java.util.WeakHashMap;

import android.content.ClipData;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.ViewParent;

public abstract class ReorderableBaseAdapter extends BaseAdapter {
	private final WeakHashMap<View, Integer> mPositions = new WeakHashMap<View, Integer>();
	private class Reference<T> {
		public Reference(T t) { reference = t; }
		T reference;
	}

	@Override
	public void notifyDataSetChanged() {
		mPositions.clear();
		super.notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetInvalidated() {
		mPositions.clear();
		super.notifyDataSetInvalidated();
	}

	@Override
	protected void bindView(View view, int position) {
		mPositions.put(view, position);
	}

	@Override
	protected void processView(View view, int type) {
		view.setOnDragListener(new View.OnDragListener() {
			@Override
			public boolean onDrag(View dstView, DragEvent event) {
				Reference<View> ref = (Reference<View>) event.getLocalState();
				final View srcView = ref.reference;

				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_ENTERED:
					srcView.setVisibility(View.VISIBLE);
					dstView.setVisibility(View.INVISIBLE);

					Integer src = mPositions.get(srcView);
					Integer dst = mPositions.get(dstView);
					if (src != null && dst != null)
						move(src, dst);

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

		view.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(final View view) {
				view.setVisibility(View.INVISIBLE);

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

	protected int getPositionFromView(View view) {
		while(true) {
			Integer position = mPositions.get(view);
			if (position != null)
				return position;

			ViewParent vp = view.getParent();
			if (vp == null || !(vp instanceof View))
				break;

			view = (View) vp;
		}

		return -1;
	}

	public abstract void move(int fromPosition, int toPosition);
}

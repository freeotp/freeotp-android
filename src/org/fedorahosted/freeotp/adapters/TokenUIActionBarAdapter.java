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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;

import android.content.Context;
import android.content.res.Resources;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

public abstract class TokenUIActionBarAdapter extends TokenUIClickAdapter {
    private final Map<View, CompoundButton> mButtons = new WeakHashMap<View, CompoundButton>();
    private final Set<Integer>              mChecked = new HashSet<Integer>();
    private ActionMode                      mActionMode;

    public TokenUIActionBarAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public void notifyDataSetChanged() {
        if (mActionMode != null)
            mActionMode.finish();
        mChecked.clear();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        if (mActionMode != null)
            mActionMode.finish();
        mChecked.clear();
        super.notifyDataSetInvalidated();
    }

    @Override
    protected void bindView(View view, int position) {
        super.bindView(view, position);
        mButtons.get(view).setChecked(mChecked.contains(position));
    }

    @Override
    protected void bindView(View view, final int position, Token token) {
        super.bindView(view, position, token);

        CompoundButton cb = (CompoundButton) view.findViewById(R.id.checkBox);
        mButtons.put(view, cb);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    mChecked.remove(position);
                    if (mChecked.size() == 0 && mActionMode != null)
                        mActionMode.finish();

                    setTitle(buttonView.getContext());
                    return;
                }

                if (mChecked.size() == 0) {
                    mActionMode = buttonView.startActionMode(new ActionMode.Callback() {
                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            return false;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            mActionMode = null;
                            for (final CompoundButton cb : mButtons.values()) {
                                cb.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        cb.setChecked(false);
                                    }
                                });
                            }
                        }

                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            menu.add(R.string.delete).setIcon(android.R.drawable.ic_menu_delete);
                            return true;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            // Get the list of all the checked positions
                            // and reverse sort the list.
                            List<Integer> list = new ArrayList<Integer>(mChecked);
                            Collections.sort(list, new Comparator<Integer>() {
                                @Override
                                public int compare(Integer lhs, Integer rhs) {
                                    return rhs.intValue() - lhs.intValue();
                                }
                            });

                            // Delete all the selected tokens (in reverse
                            // order!)
                            for (Integer i : list)
                                delete(i);

                            mode.finish();
                            return true;
                        }
                    });
                }

                mChecked.add(position);
                setTitle(buttonView.getContext());
            }
        });
    }

    private void setTitle(Context ctx) {
        if (mActionMode == null || mChecked.size() == 0)
            return;

        Resources res = ctx.getResources();
        mActionMode.setTitle(res.getQuantityString(R.plurals.tokens_selected, mChecked.size(), mChecked.size()));
    }
}

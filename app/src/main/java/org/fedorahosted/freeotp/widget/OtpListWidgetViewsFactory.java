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

package org.fedorahosted.freeotp.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;
import org.fedorahosted.freeotp.TokenPlaceholderGenerator;

import java.io.IOException;

public class OtpListWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final TokenPersistence persistence;
    private final int widgetId;

    OtpListWidgetViewsFactory(final Context context, final int widgetId) {
        this.context = context;
        this.widgetId = widgetId;
        persistence = new TokenPersistence(context);
    }

    @Override
    public int getCount() {
        return persistence.length();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        final Token token = persistence.get(position);
        final String tokenId = token.getID();
        final RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_row);

        Picasso.with(context).load(token.getImage()).error(R.drawable.logo).into(row, R.layout.widget_row, null);

        row.setTextViewText(R.id.widget_issuer, token.getIssuer());
        row.setTextViewText(R.id.widget_label, token.getLabel());

        final OtpListWidgetViewModel model = OtpListWidgetViewModel.getInstance(widgetId);
        final String code;
        final String intentAction;
        if (model.shouldShowCodeForTokenId(tokenId)) {
            code = token.generateCodes().getCurrentCode();
            persistence.save(token);
            intentAction = OtpListWidgetService.ACTION_HIDE_CODE;
        } else {
            code = TokenPlaceholderGenerator.generate(token);
            intentAction = OtpListWidgetService.ACTION_SHOW_CODE;
        }
        row.setTextViewText(R.id.widget_code, code);

        final Intent intent = new Intent()
                .setAction(intentAction)
                .putExtra(OtpListWidgetService.EXTRA_TOKEN_ID, tokenId);
        row.setOnClickFillInIntent(R.id.widget_row_container, intent);
        return row;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public void onCreate() {
        // no-op
    }

    @Override
    public void onDataSetChanged() {
        // no-op
    }

    @Override
    public void onDestroy() {
        // no-op
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}

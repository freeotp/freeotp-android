package org.fedorahosted.freeotp.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.squareup.picasso.Picasso;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPlaceholderGenerator;
import org.fedorahosted.freeotp.TokenPersistence;

import java.io.IOException;

/**
 * Created by root on 13/04/17.
 */
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
        final RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_row);

        try {
            Bitmap b = Picasso.with(context).load(token.getImage()).get();
            if (b == null) {
                row.setImageViewResource(R.id.widget_image, R.drawable.logo);
            } else {
                row.setImageViewBitmap(R.id.widget_image, b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        row.setTextViewText(R.id.widget_issuer, token.getIssuer());
        row.setTextViewText(R.id.widget_label, token.getLabel());

        final OtpListWidgetViewModel model = OtpListWidgetViewModel.getInstance(widgetId);
        final String code;
        final String intentAction;
        if (model.shouldShowCodeInPosition(position)) {
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
                .putExtra(OtpListWidgetService.EXTRA_CODE_POSITION, position);
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

package org.fedorahosted.freeotp.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.squareup.picasso.Picasso;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import java.io.IOException;

/**
 * Created by root on 13/04/17.
 */
public class OtpListWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final TokenPersistence persistence;

    public OtpListWidgetViewsFactory(final Context context) {
        this.context = context;
        persistence = new TokenPersistence(context);
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

        row.setTextViewText(R.id.widget_code, token.generateCodes().getCurrentCode());
        row.setTextViewText(R.id.widget_issuer, token.getIssuer());
        row.setTextViewText(R.id.widget_label, token.getLabel());
        return row;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}

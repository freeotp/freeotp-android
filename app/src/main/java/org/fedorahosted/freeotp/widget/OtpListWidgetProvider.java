package org.fedorahosted.freeotp.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import org.fedorahosted.freeotp.R;

/**
 * Created by root on 13/04/17.
 */
public class OtpListWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.list_widget);
        final Intent serviceIntent = new Intent(context, OtpListWidgetService.class);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        widget.setRemoteAdapter(R.id.list_widget, serviceIntent);
        appWidgetManager.updateAppWidget(appWidgetIds, widget);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}

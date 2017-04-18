package org.fedorahosted.freeotp.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by root on 13/04/17.
 */
public class OtpListWidgetService extends RemoteViewsService {
    static final String ACTION_SHOW_CODE = "org.fedorahosted.freeotp.widget.ACTION_SHOW_CODE";
    static final String ACTION_HIDE_CODE = "org.fedorahosted.freeotp.widget.ACTION_HIDE_CODE";
    static final String EXTRA_TOKEN_ID = "EXTRA_TOKEN_ID";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        return new OtpListWidgetViewsFactory(getApplicationContext(), widgetId);
    }
}

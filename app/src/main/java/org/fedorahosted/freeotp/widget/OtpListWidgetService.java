package org.fedorahosted.freeotp.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by root on 13/04/17.
 */
public class OtpListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new OtpListWidgetViewsFactory(getApplicationContext());
    }
}

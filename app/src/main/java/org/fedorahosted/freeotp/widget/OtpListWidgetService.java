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

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

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

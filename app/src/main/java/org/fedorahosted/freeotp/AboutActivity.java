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

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
    }

    @Override
    public void onStart() {
        super.onStart();

        Resources res = getResources();
        TextView tv;

        try {
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            String version = res.getString(R.string.about_version, info.versionName, info.versionCode);
            tv = (TextView) findViewById(R.id.about_version);
            tv.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String apache2 = res.getString(R.string.link_apache2);
        String license = res.getString(R.string.about_license, apache2);
        tv = (TextView) findViewById(R.id.about_license);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(license));

        String lwebsite = res.getString(R.string.link_website);
        String swebsite = res.getString(R.string.about_website, lwebsite);
        tv = (TextView) findViewById(R.id.about_website);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(swebsite));

        String problem = res.getString(R.string.link_report_a_problem);
        String help = res.getString(R.string.link_ask_for_help);
        String feedback = res.getString(R.string.about_feedback, problem, help);
        tv = (TextView) findViewById(R.id.about_feedback);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(feedback));
    }
}

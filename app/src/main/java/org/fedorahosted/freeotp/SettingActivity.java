package org.fedorahosted.freeotp;

/**
 * Created by JG on 2017-11-30.
 */

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class SettingActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
    }

    public void onStart() {
        super.onStart();

        Resources res = getResources();
        TextView tv;

        try {
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            String version = res.getString(R.string.about_version, info.versionName, info.versionCode);
            tv = findViewById(R.id.about_version);
            tv.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String apache2 = res.getString(R.string.link_apache2);
        String license = res.getString(R.string.about_license, apache2);
        tv = findViewById(R.id.about_license);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(license));

        String lwebsite = res.getString(R.string.link_website);
        String swebsite = res.getString(R.string.about_website, lwebsite);
        tv = findViewById(R.id.about_website);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(swebsite));
    }
}
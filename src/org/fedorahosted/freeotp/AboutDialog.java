package org.fedorahosted.freeotp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends AlertDialog {
	private void init(Context ctx) {
		Resources res = ctx.getResources();
		View v = getLayoutInflater().inflate(R.layout.about, null, false);
		TextView tv;

		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo info = pm.getPackageInfo(ctx.getPackageName(), 0);
			String version = res.getString(R.string.about_version,
                                           info.versionName,
                                           info.versionCode);
			tv = (TextView) v.findViewById(R.id.about_version);
			tv.setText(version);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		String apache2 = res.getString(R.string.link_apache2);
		String license = res.getString(R.string.about_license, apache2);
		tv = (TextView) v.findViewById(R.id.about_license);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(Html.fromHtml(license));

		String lwebsite = res.getString(R.string.link_website);
		String swebsite = res.getString(R.string.about_website, lwebsite);
		tv = (TextView) v.findViewById(R.id.about_website);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(Html.fromHtml(swebsite));

		String problem = res.getString(R.string.link_report_a_problem);
		String help = res.getString(R.string.link_ask_for_help);
		String feedback = res.getString(R.string.about_feedback, problem, help);
		tv = (TextView) v.findViewById(R.id.about_feedback);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(Html.fromHtml(feedback));

		String title = ctx.getResources().getString(R.string.about);
		setTitle(title + " " + ctx.getResources().getString(R.string.app_name));
		setView(v);

		String ok = res.getString(android.R.string.ok);
		setButton(BUTTON_POSITIVE, ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
	}

	public AboutDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		init(context);
	}

	public AboutDialog(Context context, int theme) {
		super(context, theme);
		init(context);
	}

	public AboutDialog(Context context) {
		super(context);
		init(context);
	}
}

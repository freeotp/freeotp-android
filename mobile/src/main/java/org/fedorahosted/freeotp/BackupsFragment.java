package org.fedorahosted.freeotp;

import static org.fedorahosted.freeotp.OnBoardingActivity.COMPLETED_ONBOARDING;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

public class BackupsFragment extends Fragment implements View.OnClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backups, container, false);

        TextView textView = view.findViewById(R.id.textViewGoogle);
        textView.setText(HtmlCompat.fromHtml(getString(R.string.google_auto_backup_link), HtmlCompat.FROM_HTML_MODE_LEGACY));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        Button doneButton = view.findViewById(R.id.button_onboard_done);
        doneButton.setOnClickListener(this::onboardDone);
        return view;
    }

    public void onboardDone(View view) {
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        sharedPreferencesEditor.putBoolean(COMPLETED_ONBOARDING, true).apply();
        requireActivity().finish();
    }

    @Override
    public void onClick(View view) {
    }
}
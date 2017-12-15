package org.fedorahosted.freeotp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import org.fedorahosted.freeotp.CredentialManager.TimeType;

public class SettingActivity extends Activity {
    private CredentialManager mCM = CredentialManager.getInstance();
    private CompoundButton.OnCheckedChangeListener mEnableListner = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            mCM.setEnable(b);
            final RadioGroup durationGroup = findViewById(R.id.setting_durationGroup);
            durationGroup.setAlpha(b ? 1 : 0.5f);
            for(int i=0; i < durationGroup.getChildCount(); i++)
                durationGroup.getChildAt(i).setClickable(b);
        }
    };
    private RadioGroup.OnCheckedChangeListener mDurationListner = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            switch(i) {
                case R.id.setting_duration10:
                    mCM.setTime(TimeType.SEC_10);
                    break;
                case R.id.setting_duration30:
                    mCM.setTime(TimeType.SEC_30);
                    break;
                case R.id.setting_duration60:
                    mCM.setTime(TimeType.SEC_60);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCM = CredentialManager.getInstance();
        setContentView(R.layout.setting);

        Switch enableSwitch = findViewById(R.id.setting_enable);
        RadioGroup durationGroup = findViewById(R.id.setting_durationGroup);

        enableSwitch.setOnCheckedChangeListener(mEnableListner);
        durationGroup.setOnCheckedChangeListener(mDurationListner);

        enableSwitch.setChecked(mCM.getEnable());
        mEnableListner.onCheckedChanged(enableSwitch, enableSwitch.isChecked());
        switch(mCM.getTime()) {
            case SEC_10:
                durationGroup.check(R.id.setting_duration10);
                break;
            case SEC_30:
                durationGroup.check(R.id.setting_duration30);
                break;
            case SEC_60:
                durationGroup.check(R.id.setting_duration60);
                break;
        }
    }
}

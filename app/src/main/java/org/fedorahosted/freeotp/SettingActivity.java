package org.fedorahosted.freeotp;

import android.app.Activity;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;
import android.view.View;

public class SettingActivity extends Activity {

    private boolean mEnableLock = false;
    private CredentialManager.TimeType mTimeType = CredentialManager.TimeType.SEC_30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        mEnableLock = CredentialManager.getInstance().getEnable();
        mTimeType = CredentialManager.getInstance().getTime();

        Switch enableLock = (Switch)findViewById(R.id.switch1);
        RadioGroup setTimeType = (RadioGroup)findViewById(R.id.radioGroup1);

        enableLock.setChecked(mEnableLock);
        switch(mTimeType)
        {
            case SEC_10:
                setTimeType.check(R.id.radioButton10);
                break;
            case SEC_30:
                setTimeType.check(R.id.radioButton30);
                break;
            case SEC_60:
                setTimeType.check(R.id.radioButton60);
                break;
        }

        enableLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked==true)
                {
                    mEnableLock = true;
                    Toast.makeText(SettingActivity.this, R.string.able_messgae, Toast.LENGTH_LONG).show();
                }
                else
                    Toast.makeText(SettingActivity.this, R.string.disable_messgae, Toast.LENGTH_LONG).show();
            }
        });

        setTimeType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.radioButton10:
                        mTimeType = CredentialManager.TimeType.SEC_10;
                        break;
                    case R.id.radioButton30:
                        mTimeType = CredentialManager.TimeType.SEC_30;
                        break;
                    case R.id.radioButton60:
                        mTimeType = CredentialManager.TimeType.SEC_60;
                        break;
                    default:
                        break;
                }
            }
        });

    }

    public void onClick_10sec(View v){
        Toast.makeText(SettingActivity.this, R.string.setting_10sec_message, Toast.LENGTH_LONG).show();
    }
    public void onClick_30sec(View v){
        Toast.makeText(SettingActivity.this, R.string.setting_30sec_message, Toast.LENGTH_LONG).show();
    }
    public void onClick_60sec(View v){
        Toast.makeText(SettingActivity.this, R.string.setting_60sec_message, Toast.LENGTH_LONG).show();
    }
    public void onClick_save(View v){
        CredentialManager.getInstance().setEnable(mEnableLock);
        CredentialManager.getInstance().setTime(mTimeType);
        Toast.makeText(SettingActivity.this, R.string.setting_save_message, Toast.LENGTH_LONG).show();
        finish();
    }

}

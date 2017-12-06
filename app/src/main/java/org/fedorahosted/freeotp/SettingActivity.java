package org.fedorahosted.freeotp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;
import android.view.View;

public class SettingActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        Switch enableLock = (Switch)findViewById(R.id.switch1);
        enableLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked==true)
                {
                    CredentialManager.getInstance().setEnable(true);
                }
                else
                    CredentialManager.getInstance().setEnable(false);
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
        Toast.makeText(SettingActivity.this, R.string.setting_save_message, Toast.LENGTH_LONG).show();
        finish();
    }

}

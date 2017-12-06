package org.fedorahosted.freeotp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import android.view.View;

public class SettingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
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
    }

}

package org.fedorahosted.freeotp;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import java.lang.UnsupportedOperationException;
import android.os.SystemClock;

import static android.content.Context.MODE_PRIVATE;


public class CredentialManager {
    private static final CredentialManager ourInstance = new CredentialManager();

    public static CredentialManager getInstance() {
        return ourInstance;
    }

    // Time user can set
    public enum TimeType {
        SEC_10, SEC_30, SEC_60
    }

    public static final int CREDENTIAL_CHECK = 2;
    public static final String SETTING_ENABLE = "Enable";
    public static final String SETTING_TIME_TYPE="Time_type";

    private Activity mActivity;
    private KeyguardManager mKeyguardManager;
    private SharedPreferences mPreferences;

    //Variable to store setting value
    private boolean mEnable = false;
    private int mTime = 30;

    //variable that last authentication point
    private long mLastCheckPass = 0;

    /*
     * init method => initialize
     * point of call => execute one time when running app.
     */
    public int init(Activity activity) {
        mActivity = activity;
        mKeyguardManager = (KeyguardManager) mActivity.getSystemService(Context.KEYGUARD_SERVICE);
        mPreferences = mActivity.getPreferences(MODE_PRIVATE);
        if(isConfigExist() && isConfigValid()) {
            loadConfig();
        }
        else {
            saveConfig();
        }
        return 0;
    }

    /*
     *  check method => check whether OTP is accessible.
     */
    public boolean check() {
        if (!mEnable || SystemClock.elapsedRealtime() - mLastCheckPass < mTime * DateUtils.SECOND_IN_MILLIS)
            return true;
        else {
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null,null);
            mActivity.startActivityForResult(intent, CREDENTIAL_CHECK);
            return false;
        }
    }

    /*
     * pass method => Called when screenLock is released successfully
     * Should only be called on MainActivity's onActivityResult
     */
    public void pass() {
        mLastCheckPass =SystemClock.elapsedRealtime();
    }

    /*
     * isConfigExist method=> Check wheter existing settings exist in SharedPreference.
     */
    private boolean isConfigExist() {
        return (mPreferences.contains(SETTING_ENABLE) && mPreferences.contains(SETTING_TIME_TYPE));
    }

    /*
     * isConfigValid method=> check whether existing SharedPreference's Settings are safe data.
     */
    private boolean isConfigValid() {
        int timeType = 0;
        timeType = mPreferences.getInt(SETTING_TIME_TYPE,-1);
        return timeType == 10 || timeType == 30 || timeType == 60;
    }

    /*
     * loadConfig method =>bring the existing settings and save the variable.
     * mEnable : Lock  => on(true) / off(false)
     */
    private  void loadConfig() {
        mEnable = mPreferences.getBoolean(SETTING_ENABLE,false);
        mTime = mPreferences.getInt(SETTING_TIME_TYPE,30);
    }

    /*
     * saveConfig method => save variable's setting with SharedPreference.
     */
    private  boolean saveConfig() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(SETTING_ENABLE, mEnable);
        editor.putInt(SETTING_TIME_TYPE, mTime);
        return editor.commit();
    }

    public boolean getEnable() {
        return mEnable;
    }

    public TimeType getTime() {
        switch (mTime) {
            case 10:
                return TimeType.SEC_10;
            case 30:
                return TimeType.SEC_30;
            case 60:
                return TimeType.SEC_60;
        }
        throw new UnsupportedOperationException("invalid TimeType");
    }

    public boolean setEnable(boolean value) {
        mEnable = value;
        return saveConfig();
    }

    public boolean setTime(TimeType value) {
        switch(value) {
            case SEC_10:
                mTime = 10;
                break;
            case SEC_30:
                mTime = 30;
                break;
            case SEC_60:
                mTime = 60;
                break;
            default:
                return false;
        }
        return saveConfig();
    }
}

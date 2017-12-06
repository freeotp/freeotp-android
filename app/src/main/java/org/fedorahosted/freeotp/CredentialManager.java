package org.fedorahosted.freeotp;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import java.lang.Exception;
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

    private Context mAppContext = null;
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
    public int init(Context appContext) {
        mAppContext = appContext;
        mKeyguardManager = (KeyguardManager)mAppContext.getSystemService(Context.KEYGUARD_SERVICE);
        mPreferences = ((Activity)mAppContext).getPreferences(MODE_PRIVATE);
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
            ((Activity) mAppContext).startActivityForResult(intent, CREDENTIAL_CHECK);
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
        if(mPreferences.contains(SETTING_ENABLE) && mPreferences.contains(SETTING_TIME_TYPE))
            return true;
        else
            return false;
    }

    /*
     * isConfigValid method=> check whether existing SharedPreference's Settings are safe data.
     */
    private boolean isConfigValid() {
        int tempTimeType = 0;
        tempTimeType = mPreferences.getInt(SETTING_TIME_TYPE,-1);
        if(tempTimeType == 10 || tempTimeType ==30 || tempTimeType ==60)
            return true;
        else
            return false;
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
        try {
            if(mEnable != mPreferences.getBoolean(SETTING_ENABLE,false))
                mPreferences.edit().putBoolean(SETTING_ENABLE,mEnable);
            if(mTime != mPreferences.getInt(SETTING_TIME_TYPE,60))
                mPreferences.edit().putInt(SETTING_TIME_TYPE,mTime);

            return true;
        } catch (Exception e) {
            loadConfig();
            return false;
        }
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
                return  TimeType.SEC_60;
        }
        throw new UnsupportedOperationException();
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

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
    private SharedPreferences mSaveSetting;

    public static CredentialManager getInstance() {
        return ourInstance;
    }

    /*
     *상수 값을 참조할 때, final로 정의함으로써, 값 참조의 효율성을 두었다.
     */
    public static final int CREDENTIAL_CHECK = 2;
    public static final String SETTING_ENABLE = "Enable";
    public static final String SETTING_TIME_TYPE="Time_type";

    // 사용자가 설정할 수 있는 시간값
    public enum TimeType {
        SEC_10, SEC_30, SEC_60
    }

    private Context mAppContext = null;
    private KeyguardManager mKeyguardManager;

    // 설정값 저장하는 변수
    private boolean mEnable = false;
    private int mTime = 30;

    // 마지막 인증 통과 시점을 저장하는 변수
    private long mLastCheckPass = 0;
    /*
     * 기능 : 초기화
     * 호출시점 : 앱 실행 시 1회. MainActivity의 onCreate 또는 객체 생성 시
     */
    public int init(Context appContext) {
        mAppContext = appContext;
        mKeyguardManager = (KeyguardManager)mAppContext.getSystemService(Context.KEYGUARD_SERVICE);
        mSaveSetting = ((Activity)mAppContext).getPreferences(MODE_PRIVATE);
        if(isConfigExist() && isConfigValid()) {
            loadConfig();
        }
        else {
            saveConfig();
        }
        return 0;
    }

    /*
     * OTP 접근 가능 여부를 검사
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
     * screenLock 해제 성공 시 호출됨
     * MainActivity의 onActivityResult에서만 호출되어야 함
     */
    public void pass() {
        mLastCheckPass =SystemClock.elapsedRealtime();
    }

    /*
     * 기존 설정이 SharedPreference에 존재하는지 확인
     */
    private boolean isConfigExist() {
        if(mSaveSetting.contains(SETTING_ENABLE) && mSaveSetting.contains(SETTING_TIME_TYPE))
            return true;
        else
            return false;
    }

    /*
     * SharedPreference의 기존 설정이 안전한 데이터인지 확인
     * timetype이 10,30,60이 아닌 경우 false
     * 없는 경우에도
     */
    private boolean isConfigValid() {
        int tempTimeType = 0;
        tempTimeType = mSaveSetting.getInt(SETTING_TIME_TYPE,-1);
        if(tempTimeType == 10 || tempTimeType ==30 || tempTimeType ==60)
            return true;
        else
            return false;
    }

    /*
     * 기존 설정을 불러와서 변수에 저장
     * mEnable : Lock기능 on(true) / off(false)
     *
     */
    private  void loadConfig() {
        mEnable = mSaveSetting.getBoolean(SETTING_ENABLE,false);
        mTime = mSaveSetting.getInt(SETTING_TIME_TYPE,30);
    }

    /*
     * 변수의 설정을 SharedPreference로 저장
     */
    private  boolean saveConfig() {
        try {
            if(mEnable != mSaveSetting.getBoolean(SETTING_ENABLE,false))
                mSaveSetting.edit().putBoolean(SETTING_ENABLE,mEnable);
            if(mTime != mSaveSetting.getInt(SETTING_TIME_TYPE,60))
                mSaveSetting.edit().putInt(SETTING_TIME_TYPE,mTime);

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

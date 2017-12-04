package org.fedorahosted.freeotp;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import java.lang.Exception;
import java.lang.UnsupportedOperationException;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;


public class CredentialManager {
    private static final CredentialManager ourInstance = new CredentialManager();
    private SharedPreferences saveSetting;

    public static CredentialManager getInstance() {
        return ourInstance;
    }

    public static final int CREDENTIAL_CHECK = 2;
    // 사용자가 설정할 수 있는 시간값
    public enum TimeType {
        SEC_10, SEC_30, SEC_60
    }

    public enum LockType{
        NONE, PATTERN, FINGER_PRINT, PASSWORD
    }
    private Context mAppContext = null;
    private KeyguardManager mKeyguardManager;

    // 설정값 저장하는 변수
    private boolean mEnable = false;
    private int mLockType = 0;
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
        saveSetting = mAppContext.getSharedPreferences("Setting",MODE_PRIVATE);
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
        if (!mEnable || new Date().getTime() - mLastCheckPass < mTime * DateUtils.SECOND_IN_MILLIS)
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
        mLastCheckPass = new Date().getTime();
    }

    /*
     * 기존 설정이 SharedPreference에 존재하는지 확인
     */
    private boolean isConfigExist() {
        if(saveSetting.contains("Enable") && saveSetting.contains("Time_type"))
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
        tempTimeType = saveSetting.getInt("Time_type",-1);

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
        mEnable = saveSetting.getBoolean("Enable",false);

        if(mEnable == true)
            mLockType = saveSetting.getInt("LockType",0);
        else
            saveSetting.edit().putBoolean("LockType",false);

        mTime = saveSetting.getInt("Time_type",30);
    }

    /*
     * 변수의 설정을 SharedPreference로 저장
     */
    private  boolean saveConfig() {
        try {
            if(mEnable != saveSetting.getBoolean("Enable",false))
                saveSetting.edit().putBoolean("Enable",mEnable);
            if(mLockType != saveSetting.getInt("LockType",0))
                saveSetting.edit().putInt("LockType",mLockType);
            if(mTime != saveSetting.getInt("Time_type",30))
                saveSetting.edit().putInt("Time_type",mTime);

            return true;
        } catch (Exception e) {
            loadConfig();
            return false;
        }
    }

    public boolean getEnable() {
        return mEnable;
    }

    public LockType getLockType() {
        switch(mLockType) {
            case 0:
                return LockType.NONE;
            case 1:
                return LockType.PATTERN;
            case 2:
                return LockType.FINGER_PRINT;
            case 3:
                return LockType.PASSWORD;
        }
        throw new UnsupportedOperationException();
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

    public boolean setLockType(Activity mainActivity, LockType value) {
        switch(value) {
            case NONE:
                mLockType = 0;
                break;
            case PATTERN:
                mLockType = 1;
                break;
            case FINGER_PRINT:
                mLockType = 2;
                break;
            case PASSWORD:
                mLockType = 3;
                break;
            default :
                return false;
        }
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

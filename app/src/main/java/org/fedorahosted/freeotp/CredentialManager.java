package org.fedorahosted.freeotp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

    // 사용자가 설정할 수 있는 시간값
    public enum TimeType {
        SEC_10, SEC_30, SEC_60
    }
    private Context mAppContext = null;

    // 설정값 저장하는 변수
    private boolean mEnable = false;
    private boolean mUseFingerprint = false;
    private int mTime = 30;

    // 마지막 인증 통과 시점을 저장하는 변수
    private long mLastCheckPass = Long.MIN_VALUE;
    /*
     * 기능 : 초기화
     * 호출시점 : 앱 실행 시 1회. MainActivity의 onCreate 또는 객체 생성 시
     */
    public int init(Context appContext) {
        mAppContext = appContext;
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
        //기능 완성시까지 true 리턴
        //unreachable statement 에러 방지 위해 if문 사용
        if(true) return true;

        if (!mEnable || new Date().getTime() - mLastCheckPass < mTime * DateUtils.SECOND_IN_MILLIS)
            return true;
        else {
            //TODO : 설정에 따라 지문 혹은 패턴 혹은 패스워드 요구
            //TODO : 성공시 mLastCheckPass 갱신. 성공여부를 리턴
            throw new UnsupportedOperationException();
        }
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

        if(tempTimeType != 10 || tempTimeType !=20 || tempTimeType !=30)
            return false;
        else
            return true;
    }

    /*
     * 기존 설정을 불러와서 변수에 저장
     * mEnable : Lock기능 on(true) / off(false)
     *
     */
    private  void loadConfig() {
        mEnable = saveSetting.getBoolean("Enable",false);

            if(mEnable == true)
                mUseFingerprint = saveSetting.getBoolean("UseFingerPrint",false);
            else
                saveSetting.edit().putBoolean("UseFingerPrint",false);

        mTime = saveSetting.getInt("Time_type",30);
    }

    /*
     * 변수의 설정을 SharedPreference로 저장
     */
    private  boolean saveConfig() {
        try {
            if(mEnable != saveSetting.getBoolean("Enable",false))
                saveSetting.edit().putBoolean("Enable",mEnable);
            if(mUseFingerprint != saveSetting.getBoolean("UseFingerPrint",false))
                saveSetting.edit().putBoolean("UseFingerPrint",mUseFingerprint);
            if(mTime != saveSetting.getInt("Time_type",30))
                saveSetting.edit().putInt("Time_type",mTime);

            return true;
        } catch (Exception e) {
            loadConfig();
            return false;
        }
    }

    /*
     * USE_FINGERPRINT 권한 확인
     */
    private boolean isPermission() {
        final int PERMISSION = ContextCompat.checkSelfPermission(mAppContext,
                                        Manifest.permission.USE_FINGERPRINT);
        return  PERMISSION == PackageManager.PERMISSION_GRANTED;
    }

    /*
     * USE_FINGERPRINT 권한 요청
     */
    private  void tryPermission(Activity mainActivity) {
        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.USE_FINGERPRINT}, 2);
    }


    public boolean getEnable() {
        return mEnable;
    }

    public boolean getUseFingerprint() {
        return mUseFingerprint;
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

    public boolean setUseFingerprint(Activity mainActivity, boolean value) {
        if(value && !isPermission()) {
            tryPermission(mainActivity);
            return false;
        }
        mUseFingerprint = value;
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

package org.fedorahosted.freeotp;

import android.text.format.DateUtils;
import java.lang.Exception;
import java.lang.UnsupportedOperationException;
import java.util.Date;

public class CredentialManager {
    private static final CredentialManager ourInstance = new CredentialManager();

    public static CredentialManager getInstance() {
        return ourInstance;
    }

    // 사용자가 설정할 수 있는 시간값
    public enum TimeType {
        SEC_10, SEC_30, SEC_60
    }

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
    public int init() {
        if(isConfigExist() && isConfigValid()) {
            loadConfig();
        }
        else {
            saveConfig();
        }
        throw new UnsupportedOperationException();
    }

    /*
     * OTP 접근 가능 여부를 검사
     */
    public boolean check() {
        if (!mEnable || new Date().getTime() - mLastCheckPass < mTime * DateUtils.SECOND_IN_MILLIS)
            return  true;
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
        throw new UnsupportedOperationException();
    }

    /*
     * SharedPreference의 기존 설정이 안전한 데이터인지 확인
     */
    private boolean isConfigValid() {
        throw new UnsupportedOperationException();
    }

    /*
     * 기존 설정을 불러와서 변수에 저장
     */
    private  void loadConfig() {

    }

    /*
     * 변수의 설정을 SharedPreference로 저장
     */
    private  boolean saveConfig() {
        try {
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
        throw new UnsupportedOperationException();
    }

    /*
     * USE_FINGERPRINT 권한 요청
     */
    private  boolean tryPermission() {
        throw new UnsupportedOperationException();
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

    public boolean setUseFingerprint(boolean value) {
        if(value && !isPermission() && !tryPermission())
            return false;

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

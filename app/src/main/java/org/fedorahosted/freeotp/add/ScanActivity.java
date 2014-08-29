/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp.add;

import java.util.List;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ScanActivity extends Activity implements SurfaceHolder.Callback {
    private final CameraInfo    mCameraInfo  = new CameraInfo();
    private final ScanAsyncTask mScanAsyncTask;
    private final int           mCameraId;
    private Handler             mHandler;
    private Camera              mCamera;

    private static class AutoFocusHandler extends Handler implements Camera.AutoFocusCallback {
        private final Camera mCamera;

        public AutoFocusHandler(Camera camera) {
            mCamera = camera;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mCamera.autoFocus(this);
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            sendEmptyMessageDelayed(0, 1000);
        }
    }

    public static boolean haveCamera() {
        return findCamera(new CameraInfo()) >= 0;
    }

    private static int findCamera(CameraInfo cameraInfo) {
        int cameraId = Camera.getNumberOfCameras() - 1;

        // Find a back-facing camera, otherwise use the first camera.
        for (; cameraId > 0; cameraId--) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK)
                break;
        }

        return cameraId;
    }

    public ScanActivity() {
        super();

        mCameraId = findCamera(mCameraInfo);
        assert mCameraId >= 0;

        // Create the decoder thread
        mScanAsyncTask = new ScanAsyncTask() {
            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                Token token = TokenPersistence.addWithToast(ScanActivity.this, result);
                if (token == null || token.getImage() == null) {
                    finish();
                    return;
                }

                final ImageView image = (ImageView) findViewById(R.id.image);
                Picasso.with(ScanActivity.this)
                        .load(token.getImage())
                        .placeholder(R.drawable.scan)
                        .into(image, new Callback() {
                            @Override
                            public void onSuccess() {
                                findViewById(R.id.progress).setVisibility(View.INVISIBLE);
                                image.setAlpha(0.9f);
                                image.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                }, 2000);
                            }

                            @Override
                            public void onError() {
                                finish();
                            }
                        });
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);
        mScanAsyncTask.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScanAsyncTask.cancel(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ((SurfaceView) findViewById(R.id.surfaceview)).getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera == null)
            return;

        // The code in this section comes from the developer docs. See:
        // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)

        int rotation = 0;
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_0:
            rotation = 0;
            break;
        case Surface.ROTATION_90:
            rotation = 90;
            break;
        case Surface.ROTATION_180:
            rotation = 180;
            break;
        case Surface.ROTATION_270:
            rotation = 270;
            break;
        }

        int result = 0;
        switch (mCameraInfo.facing) {
        case Camera.CameraInfo.CAMERA_FACING_FRONT:
            result = (mCameraInfo.orientation + rotation) % 360;
            result = (360 - result) % 360; // compensate the mirror
            break;

        case Camera.CameraInfo.CAMERA_FACING_BACK:
            result = (mCameraInfo.orientation - rotation + 360) % 360;
            break;
        }

        mCamera.setDisplayOrientation(result);
        mCamera.startPreview();

        if (mHandler != null)
            mHandler.sendEmptyMessageDelayed(0, 100);
    }

    @Override
    @TargetApi(14)
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceDestroyed(holder);

        try {
            // Open the camera
            mCamera = Camera.open(mCameraId);
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(mScanAsyncTask);
        } catch (Exception e) {
            e.printStackTrace();
            surfaceDestroyed(holder);

            // Show error message
            findViewById(R.id.surfaceview).setVisibility(View.INVISIBLE);
            findViewById(R.id.progress).setVisibility(View.INVISIBLE);
            findViewById(R.id.window).setVisibility(View.INVISIBLE);
            findViewById(R.id.textview).setVisibility(View.VISIBLE);
            return;
        }

        // Set auto-focus mode
        Parameters params = mCamera.getParameters();
        List<String> modes = params.getSupportedFocusModes();
        if (modes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        else if (modes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        else if (modes.contains(Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            mHandler = new AutoFocusHandler(mCamera);
        }
        mCamera.setParameters(params);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera == null)
            return;

        if (mHandler != null) {
            mCamera.cancelAutoFocus();
            mHandler.removeMessages(0);
            mHandler = null;
        }

        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }
}

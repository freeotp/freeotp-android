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

package org.fedorahosted.freeotp.dialogs;

import java.util.List;

import org.fedorahosted.freeotp.R;

import android.annotation.TargetApi;
import android.content.Intent;
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
import android.widget.TextView;

public class CameraDialogActivity extends BaseAddTokenDialogActivity implements SurfaceHolder.Callback {
    private final CameraInfo            mCameraInfo  = new CameraInfo();
    private final CameraDecodeAsyncTask mDecodeAsyncTask;
    private final int                   mCameraId;
    private Handler                     mHandler;
    private Camera                      mCamera;

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

    public CameraDialogActivity() {
        super(R.layout.camera,
              android.R.string.cancel, R.string.manual_entry, 0);

        // Find a back-facing camera, otherwise use the first camera.
        int cameraId;
        for (cameraId = Camera.getNumberOfCameras() - 1; cameraId > 0; cameraId--) {
            Camera.getCameraInfo(cameraId, mCameraInfo);
            if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK)
                break;
        }
        mCameraId = cameraId;

        // Create the decoder thread
        mDecodeAsyncTask = new CameraDecodeAsyncTask() {
            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                onTokenURI(result);
                finish();
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If we have no cameras, open the manual dialog
        if (mCameraId < 0) {
            startActivity(new Intent(this, ManualDialogActivity.class));
            finish();
            return;
        }

        mDecodeAsyncTask.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDecodeAsyncTask.cancel(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SurfaceView sv = (SurfaceView) findViewById(R.id.camera_surfaceview);
        sv.getHolder().addCallback(this);
    }

    @Override
    public void onClick(View view, int which) {
        if (which != BUTTON_NEUTRAL)
            return;

        startActivity(new Intent(CameraDialogActivity.this, ManualDialogActivity.class));
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
            mCamera.setPreviewCallback(mDecodeAsyncTask);
        } catch (Exception e) {
            e.printStackTrace();
            surfaceDestroyed(holder);

            // Show error message
            SurfaceView sv = (SurfaceView) findViewById(R.id.camera_surfaceview);
            TextView tv = (TextView) findViewById(R.id.camera_textview);
            sv.setVisibility(View.INVISIBLE);
            tv.setVisibility(View.VISIBLE);
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

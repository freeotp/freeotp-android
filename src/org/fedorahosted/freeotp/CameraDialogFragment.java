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

package org.fedorahosted.freeotp;

import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class CameraDialogFragment extends BaseAlertDialogFragment implements SurfaceHolder.Callback {
	public static final String FRAGMENT_TAG = "fragment_camera";

	private final CameraInfo mCameraInfo = new CameraInfo();
	private final DecodeAsyncTask mDecodeAsyncTask;
	private final int mCameraId;
	private Camera mCamera;

	public CameraDialogFragment() {
		super(R.string.scan_qr_code, R.layout.camera,
			android.R.string.cancel, R.string.manual_entry, 0);

		// Find a back-facing camera
		int cameraId;
		for (cameraId = Camera.getNumberOfCameras() - 1; cameraId >= 0; cameraId--) {
			Camera.getCameraInfo(cameraId, mCameraInfo);
			if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK)
				break;
		}
		mCameraId = cameraId;

		// Create the decoder thread
		mDecodeAsyncTask = new DecodeAsyncTask() {
			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				if (result != null)
					((MainActivity) getActivity()).tokenURIReceived(result);
				dismiss();
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// If we have no back facing camera, open the manual dialog
		if (mCameraId < 0) {
			new ManualDialogFragment().show(getFragmentManager(),
					ManualDialogFragment.FRAGMENT_TAG);
			dismiss();
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
	protected void onViewInflated(View view) {
		SurfaceView sv = (SurfaceView) view.findViewById(R.id.camera_surfaceview);
		sv.getHolder().addCallback(this);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which != AlertDialog.BUTTON_NEUTRAL)
			return;

		new ManualDialogFragment().show(getFragmentManager(),
				ManualDialogFragment.FRAGMENT_TAG);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mCamera == null)
			return;

		int rotation = 0;
		switch (getActivity().getWindowManager().getDefaultDisplay().getRotation()) {
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

		mCamera.setDisplayOrientation((mCameraInfo.orientation - rotation + 360) % 360);
		mCamera.startPreview();
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
			Dialog d = getDialog();
			SurfaceView sv = (SurfaceView) d.findViewById(R.id.camera_surfaceview);
			TextView tv = (TextView) d.findViewById(R.id.camera_textview);
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
		else if (modes.contains(Parameters.FOCUS_MODE_AUTO))
			params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
		mCamera.setParameters(params);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera == null)
			return;

		mCamera.stopPreview();
		mCamera.setPreviewCallback(null);
		mCamera.release();
		mCamera = null;
	}
}

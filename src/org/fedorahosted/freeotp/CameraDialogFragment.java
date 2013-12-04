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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class CameraDialogFragment extends BaseAlertDialogFragment implements SurfaceHolder.Callback {
	public static final String FRAGMENT_TAG = "fragment_camera";

	private DecodeAsyncTask mDecodeAsyncTask;
	private Camera mCamera;

	public CameraDialogFragment() {
		super(R.string.scan_qr_code, R.layout.camera,
			android.R.string.cancel, R.string.manual_entry, 0);
	}

	@Override
	protected void onViewInflated(View view) {
		SurfaceView sv = (SurfaceView) view.findViewById(R.id.camera_surfaceview);
		sv.getHolder().addCallback(this);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == AlertDialog.BUTTON_NEUTRAL) {
			new AddTokenDialog(getActivity()) {
				@Override
				public void addToken(String uri) {
					((MainActivity) getActivity()).tokenURIReceived(uri);
				}
			}.show();
		}
	}

	@Override
	public void onDestroyView() {
		if (mDecodeAsyncTask != null)
			mDecodeAsyncTask.cancel(true);

		super.onDestroyView();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mCamera == null)
			return;

		WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
		switch (wm.getDefaultDisplay().getRotation()) {
		case Surface.ROTATION_0:
			mCamera.setDisplayOrientation(90);
			break;
		case Surface.ROTATION_270:
			mCamera.setDisplayOrientation(180);
			break;
		}

		mCamera.startPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mCamera = Camera.open();
			mCamera.setPreviewDisplay(holder);

			// Create the decoder thread
			mDecodeAsyncTask = new DecodeAsyncTask() {
				@Override
				protected void onPostExecute(String result) {
					super.onPostExecute(result);
					if (result != null)
						((MainActivity) getActivity()).tokenURIReceived(result);
					mDecodeAsyncTask = null;
					dismiss();
				}
			};
			mDecodeAsyncTask.execute();
			mCamera.setPreviewCallback(mDecodeAsyncTask);

			// Set auto-focus mode
			Parameters params = mCamera.getParameters();
			params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			mCamera.setParameters(params);
		} catch (Exception e) {
			SurfaceView sv = (SurfaceView) getDialog().findViewById(R.id.camera_surfaceview);
			sv.setVisibility(View.INVISIBLE);
			TextView tv = (TextView) getDialog().findViewById(R.id.camera_textview);
			tv.setVisibility(View.VISIBLE);
			e.printStackTrace();
		}
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

/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 * Authors: Siemens AG <max.wittig@siemens.com>
 *
 * Copyright (C) 2013-2018  Nathaniel McCallum, Red Hat
 * Copyright (C) 2017  Max Wittig, Siemens AG
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

package org.fedorahosted.freeotp.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import org.fedorahosted.freeotp.R;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalLensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanDialogFragment extends AppCompatDialogFragment implements ImageAnalysis.Analyzer {
    private static final String LOGTAG = "ScanDialogFragment";

    private ProgressBar mProgress;
    private PreviewView mCamera;
    private ImageView mImage;
    private TextView mError;

    ExecutorService mCameraExecutor = null;
    private ProcessCameraProvider mCameraProvider = null;

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = View.inflate(getContext(), R.layout.fragment_scan, null);

        mProgress = v.findViewById(R.id.progress);
        mCamera = v.findViewById(R.id.camera);
        mImage = v.findViewById(R.id.image);
        mError = v.findViewById(R.id.error);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        switch (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)) {
            case PackageManager.PERMISSION_GRANTED:
                onRequestPermissionsResult(0,
                        new String[] { Manifest.permission.CAMERA },
                        new int[] { PackageManager.PERMISSION_GRANTED });
                break;

            default:
                requestPermissions(new String[] { Manifest.permission.CAMERA }, 0);
                break;
        }
    }

    @OptIn(markerClass = ExperimentalLensFacing.class)
    @CameraSelector.LensFacing
    private int getLensFacing(@NonNull ProcessCameraProvider cameraProvider)
            throws CameraInfoUnavailableException, RuntimeException {
        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            return CameraSelector.LENS_FACING_BACK;
        }

        CameraSelector externalCameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL).build();
        if (cameraProvider.hasCamera(externalCameraSelector)) {
            return CameraSelector.LENS_FACING_EXTERNAL;
        }

        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            return CameraSelector.LENS_FACING_FRONT;
        }

        throw new RuntimeException("Can't retrieve camera lens facing");
    }

    private void startCamera() {
        mCameraExecutor = Executors.newSingleThreadExecutor();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity());
        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                imageAnalysis.setAnalyzer(mCameraExecutor, this);

                Preview preview = new Preview.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(getLensFacing(mCameraProvider)).build();

                mCamera.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
                preview.setSurfaceProvider(mCamera.getSurfaceProvider());

                mCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException | CameraInfoUnavailableException e) {
                // Unexpected
                Log.e(LOGTAG, "Unexpected error: " + e.getMessage());
                showError();
            }
        }, ContextCompat.getMainExecutor(requireActivity()));
    }

    private void stopCamera() {
        requireActivity().runOnUiThread(() -> {
            if (mCameraProvider != null) {
                mCameraProvider.unbindAll();
            }

            mCameraExecutor.shutdown();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (!permissions[i].equals(Manifest.permission.CAMERA))
                continue;

            switch (grantResults[i]) {
                case PackageManager.PERMISSION_GRANTED:
                    startCamera();
                    break;

                default:
                    dismiss();
                    break;
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopCamera();
    }

    @NonNull
    private static LuminanceSource getLuminanceSource(@NonNull ImageProxy imageProxy) {
        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] i = new byte[buffer.remaining()];
        buffer.get(i);

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();

        return new PlanarYUVLuminanceSource(i, width, height, 0, 0, width, height, false);
    }

    private void vibrate() {
        Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(250);
            }
        }
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        LuminanceSource ls = getLuminanceSource(imageProxy);

        try {
            final BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(ls));
            final List<BarcodeFormat> formats = List.of(BarcodeFormat.QR_CODE);
            final Map<DecodeHintType,?> hints = Map.of(
                    DecodeHintType.POSSIBLE_FORMATS, formats,
                    DecodeHintType.ALSO_INVERTED, Boolean.TRUE
            );
            final String uri = new MultiFormatReader().decode(bb, hints).getText();

            int size = mImage.getWidth();
            if (size > mImage.getHeight())
                size = mImage.getHeight();

            BitMatrix bm = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, size, size);
            stopCamera();

            final Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    b.setPixel(x, y, bm.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            vibrate();

            mImage.post(() -> {
                mProgress.setVisibility(View.INVISIBLE);
                mCamera.animate()
                    .setInterpolator(new DecelerateInterpolator())
                    .setDuration(2000)
                    .alpha(0.0f)
                    .start();

                mImage.setImageBitmap(b);
                mImage.animate()
                    .setInterpolator(new DecelerateInterpolator())
                    .setDuration(2000)
                    .alpha(1.0f)
                    .withEndAction(() -> {
                        mImage.post(() -> {
                            Activity a = (Activity) requireActivity();
                            a.addToken(Uri.parse(uri), true);
                        });
                        dismiss();
                    })
                    .start();
            });
        } catch (NotFoundException | WriterException e) {
            Log.e(LOGTAG, "Exception", e);
        }

        imageProxy.close();
    }

    public void showError() {
        mProgress.setVisibility(View.INVISIBLE);
        mCamera.setVisibility(View.INVISIBLE);
        mImage.setVisibility(View.INVISIBLE);
        mError.setVisibility(View.VISIBLE);
    }
}

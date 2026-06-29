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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import org.fedorahosted.freeotp.databinding.FragmentScanBinding;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanDialogFragment extends AppCompatDialogFragment implements ImageAnalysis.Analyzer {
    private static final String LOGTAG = "ScanDialogFragment";

    private FragmentScanBinding mBinding;
    private ActivityResultLauncher<String> mRequestPermissionLauncher;

    ExecutorService mCameraExecutor = null;
    private ProcessCameraProvider mCameraProvider = null;

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    dismiss();
                }
            });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentScanBinding.inflate(inflater);
        return mBinding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            mRequestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @OptIn(markerClass = ExperimentalLensFacing.class)
    @CameraSelector.LensFacing
    private int getLensFacing(@NonNull ProcessCameraProvider cameraProvider)
            throws CameraInfoUnavailableException, RuntimeException {
        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            return CameraSelector.LENS_FACING_BACK;
        }

        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            return CameraSelector.LENS_FACING_FRONT;
        }

        CameraSelector externalCameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL).build();
        if (cameraProvider.hasCamera(externalCameraSelector)) {
            return CameraSelector.LENS_FACING_EXTERNAL;
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

                mBinding.camera.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
                preview.setSurfaceProvider(mBinding.camera.getSurfaceProvider());

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

    private Bitmap createBlankBitmap(int size)
    {
        final Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        b.eraseColor(Color.WHITE);
        return b;
    }

    private Bitmap createUriBitmap(String uri, int size)
    {
        // ZXing refuses to encode an empty string
        if (uri == null || uri.isEmpty()) {
            return createBlankBitmap(size);
        }

        try {
            BitMatrix bm = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, size, size);

            final Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    b.setPixel(x, y, bm.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return b;
        } catch (WriterException e) {
            Log.e(LOGTAG, "Exception", e);
            return createBlankBitmap(size);
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

            int size = mBinding.image.getWidth();
            if (size > mBinding.image.getHeight())
                size = mBinding.image.getHeight();

            final Bitmap b = createUriBitmap(uri, size);
            stopCamera();
            vibrate();

            mBinding.image.post(() -> {
                mBinding.progress.setVisibility(View.INVISIBLE);
                mBinding.camera.animate()
                    .setInterpolator(new DecelerateInterpolator())
                    .setDuration(2000)
                    .alpha(0.0f)
                    .start();

                mBinding.image.setImageBitmap(b);
                mBinding.image.animate()
                    .setInterpolator(new DecelerateInterpolator())
                    .setDuration(2000)
                    .alpha(1.0f)
                    .withEndAction(() -> {
                        mBinding.image.post(() -> {
                            Activity a = (Activity) requireActivity();
                            a.addToken(Uri.parse(uri), true);
                        });
                        dismiss();
                    })
                    .start();
            });
        } catch (NotFoundException e) {
            Log.e(LOGTAG, "Exception", e);
        }

        imageProxy.close();
    }

    public void showError() {
        mBinding.progress.setVisibility(View.INVISIBLE);
        mBinding.camera.setVisibility(View.INVISIBLE);
        mBinding.image.setVisibility(View.INVISIBLE);
        mBinding.error.setVisibility(View.VISIBLE);
    }
}

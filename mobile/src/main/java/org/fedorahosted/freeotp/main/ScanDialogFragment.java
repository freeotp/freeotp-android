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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;

import org.fedorahosted.freeotp.R;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.Resolution;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.view.CameraView;

import static io.fotoapparat.selector.FocusModeSelectorsKt.autoFocus;
import static io.fotoapparat.selector.FocusModeSelectorsKt.continuousFocusPicture;
import static io.fotoapparat.selector.FocusModeSelectorsKt.fixed;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;
import static io.fotoapparat.selector.LensPositionSelectorsKt.external;
import static io.fotoapparat.selector.LensPositionSelectorsKt.front;
import static io.fotoapparat.selector.SelectorsKt.firstAvailable;

public class ScanDialogFragment extends AppCompatDialogFragment
        implements FrameProcessor, CameraErrorListener {
    private Fotoapparat mFotoapparat;
    private ProgressBar mProgress;
    private CameraView mCamera;
    private ImageView mImage;
    private TextView mError;

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = View.inflate(getContext(), R.layout.fragment_scan, null);

        mProgress = v.findViewById(R.id.progress);
        mCamera = v.findViewById(R.id.camera);
        mImage = v.findViewById(R.id.image);
        mError = v.findViewById(R.id.error);

        mFotoapparat = Fotoapparat.with(getContext())
            .focusMode(firstAvailable(continuousFocusPicture(), autoFocus(), fixed()))
            .lensPosition(firstAvailable(back(), external(), front()))
            .previewScaleType(ScaleType.CenterCrop)
            .cameraErrorCallback(this)
            .frameProcessor(this)
            .into(mCamera)
            .build();

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        switch (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)) {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (!permissions[i].equals(Manifest.permission.CAMERA))
                continue;

            switch (grantResults[i]) {
                case PackageManager.PERMISSION_GRANTED:
                    mFotoapparat.start();
                    mCamera.animate()
                        .setInterpolator(new AccelerateInterpolator())
                        .setDuration(2000)
                        .alpha(1.0f)
                        .start();
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
        mFotoapparat.stop();
    }

    @Override
    public void process(Frame frame) {
        byte[] i = frame.getImage();
        Resolution r = frame.getSize();

        LuminanceSource ls = new PlanarYUVLuminanceSource(
                i, r.width, r.height, 0, 0, r.width, r.height, false);

        try {
            BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(ls));
            final String uri = new QRCodeReader().decode(bb).getText();

            int size = mImage.getWidth();
            if (size > mImage.getHeight())
                size = mImage.getHeight();

            BitMatrix bm = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, size, size);
            mFotoapparat.stop();

            final Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    b.setPixel(x, y, bm.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(250);
            }

            mImage.post(new Runnable() {
                @Override
                public void run() {
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
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                mImage.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Activity a = (Activity) getActivity();
                                        a.addToken(Uri.parse(uri), true);
                                    }
                                });
                                dismiss();
                            }
                        })
                        .start();
                }
            });
        } catch (NotFoundException | ChecksumException | FormatException | WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(CameraException e) {
        mProgress.setVisibility(View.INVISIBLE);
        mCamera.setVisibility(View.INVISIBLE);
        mImage.setVisibility(View.INVISIBLE);
        mError.setVisibility(View.VISIBLE);
    }
}

/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 * Authors: Siemens AG <max.wittig@siemens.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
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

package org.fedorahosted.freeotp.add;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.parameter.selector.FocusModeSelectors;
import io.fotoapparat.view.CameraView;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.autoFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.fixed;
import static io.fotoapparat.parameter.selector.LensPositionSelectors.back;
import static io.fotoapparat.parameter.selector.Selectors.firstAvailable;
import static io.fotoapparat.parameter.selector.SizeSelectors.biggestSize;

public class ScanActivity extends Activity {
    private CameraView cameraView;
    private Fotoapparat fotoapparat;
    private ScanFrameProcessor scanFrameProcessor;
    private static ScanBroadcastReceiver receiver;

    public class ScanBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION = "org.fedorahosted.freeotp.ACTION_CODE_SCANNED";
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("scanResult");
            addTokenAndFinish(text);
        }
    }

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void addTokenAndFinish(String text) {
        Token token = TokenPersistence.addWithToast(ScanActivity.this, text);
        if (token == null || token.getImage() == null) {
            finish();
            return;
        }

        final ImageView image = findViewById(R.id.image);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(receiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new ScanBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter(ScanBroadcastReceiver.ACTION));
        setContentView(R.layout.scan);
        cameraView = findViewById(R.id.camera_view);
        scanFrameProcessor = new ScanFrameProcessor(this);

        fotoapparat = Fotoapparat
                .with(this)
                .into(cameraView)           // view which will draw the camera preview
                .previewScaleType(ScaleType.CENTER_CROP)  // we want the preview to fill the view
                .photoSize(biggestSize())   // we want to have the biggest photo possible
                .lensPosition(back())       // we want back camera
                .focusMode(firstAvailable(  // (optional) use the first focus mode which is supported by device
                        FocusModeSelectors.continuousFocus(),
                        autoFocus(),        // in case if continuous focus is not available on device, auto focus will be used
                        fixed()             // if even auto focus is not available - fixed focus mode will be used
                ))
                .frameProcessor(scanFrameProcessor)   // (optional) receives each frame from preview stream
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fotoapparat.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fotoapparat.stop();
    }
}

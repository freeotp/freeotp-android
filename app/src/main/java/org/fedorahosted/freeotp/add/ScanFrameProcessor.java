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

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;

public class ScanFrameProcessor implements FrameProcessor {

    private static Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());
    private Reader reader;
    private Context scanActivityContext;

    public ScanFrameProcessor(Context context) {
        scanActivityContext = context;
    }

    @Override
    public void processFrame(final Frame frame) {
        MAIN_THREAD_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                try {
                    reader = new QRCodeReader();
                    LuminanceSource ls = new PlanarYUVLuminanceSource(
                            frame.image, frame.size.width, frame.size.height,
                            0, 0, frame.size.width, frame.size.height, false);
                    Result r = reader.decode(new BinaryBitmap(new HybridBinarizer(ls)));
                    sendTextToActivity(r.getText());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendTextToActivity(String text) {
        Intent intent = new Intent();
        intent.setAction(ScanActivity.ScanBroadcastReceiver.ACTION);
        intent.putExtra("scanResult", text);
        scanActivityContext.sendBroadcast(intent);
    }
}

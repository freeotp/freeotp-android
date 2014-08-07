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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class ScanAsyncTask extends AsyncTask<Void, Void, String> implements PreviewCallback {
    private static class Data {
        public byte[] data;
        Camera.Size   size;
    }

    private final BlockingQueue<Data> mBlockingQueue;
    private final Reader              mReader;

    public ScanAsyncTask() {
        mBlockingQueue = new LinkedBlockingQueue<Data>(5);
        mReader = new QRCodeReader();
    }

    @Override
    protected String doInBackground(Void... args) {
        while (true) {
            try {
                Data data = mBlockingQueue.take();
                LuminanceSource ls = new PlanarYUVLuminanceSource(
                        data.data, data.size.width, data.size.height,
                        0, 0, data.size.width, data.size.height, false);
                Result r = mReader.decode(new BinaryBitmap(new HybridBinarizer(ls)));
                return r.getText();
            } catch (InterruptedException e) {
                return null;
            } catch (NotFoundException e) {
            } catch (ChecksumException e) {
            } catch (FormatException e) {
            } catch (ArrayIndexOutOfBoundsException e) {
            } finally {
                mReader.reset();
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Data d = new Data();
        d.data = data;
        d.size = camera.getParameters().getPreviewSize();
        mBlockingQueue.offer(d);
    }
}

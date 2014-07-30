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

package org.fedorahosted.freeotp.adapters;

import org.fedorahosted.freeotp.ProgressCircle;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.Token.TokenType;
import org.fedorahosted.freeotp.TokenCode;
import org.fedorahosted.freeotp.TokenPersistence;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class TokenUIClickAdapter extends TokenUIBaseAdapter {
    private static class Ticker extends Handler {
        private static class Task {
            public final View mView;
            public final TextView mCode;
            public final ImageView mImage;
            public final ProgressCircle mProgress;
            public final TokenCode mCodes;
            public final TokenType mType;
            public final long mStartTime;
            public Task(View view, TokenCode codes, TokenType type) {
                mView = view;
                mCode = (TextView) view.findViewById(R.id.code);
                mImage = (ImageView) view.findViewById(R.id.image);
                mProgress = (ProgressCircle) view.findViewById(R.id.progress);
                mCodes = codes;
                mType = type;
                mStartTime = System.currentTimeMillis();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            Task task = (Task) msg.obj;

            // Get the current data
            String code = task.mCodes.getCurrentCode();
            int inner = task.mCodes.getCurrentProgress();
            int outer = task.mCodes.getTotalProgress();

            // If all tokens are expired, reset and return.
            if (code == null) {
                stop(task.mView, task.mCode.getText().length());
                return;
            }

            // Determine whether to enable/disable the view.
            boolean enabled = false;
            enabled |= task.mType != TokenType.HOTP;
            enabled |= System.currentTimeMillis() - task.mStartTime > 5000;
            task.mView.setEnabled(enabled);

            // Update the fields
            task.mCode.setText(code);
            task.mProgress.setOuter(task.mType == TokenType.TOTP);
            task.mProgress.setProgress(inner, outer);

            // Sleep
            sendMessageDelayed(Message.obtain(msg), 100);
        }

        public void start(View view, TokenCode codes, TokenType type) {
            Task task = new Task(view, codes, type);
            task.mImage.setVisibility(View.GONE);
            task.mProgress.setVisibility(View.VISIBLE);

            removeMessages(System.identityHashCode(view));
            Message.obtain(this, System.identityHashCode(view), task).sendToTarget();
        }

        public void stop(View view, int placeholderLength) {
            char[] placeholder = new char[placeholderLength];
            for (int i = 0; i < placeholder.length; i++)
                placeholder[i] = '-';

            ((TextView) view.findViewById(R.id.code)).setText(new String(placeholder));
            view.findViewById(R.id.image).setVisibility(View.VISIBLE);
            view.findViewById(R.id.progress).setVisibility(View.GONE);
            view.setEnabled(true);

            removeMessages(System.identityHashCode(view));
        }
    }

    private final Ticker mTicker = new Ticker();

    public TokenUIClickAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    protected void bindView(View view, int position, final Token token) {
        super.bindView(view, position, token);
        mTicker.stop(view, token.getDigits());

        // Update click listener
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TokenCode codes = token.generateCodes();
                new TokenPersistence(v.getContext()).save(token);

                mTicker.start(v, codes, token.getType());
            }
        });
    }
}

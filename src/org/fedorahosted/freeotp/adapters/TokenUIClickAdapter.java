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

import org.fedorahosted.freeotp.CircleProgressBar;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.Token.TokenType;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class TokenUIClickAdapter extends TokenUIBaseAdapter {
    private static class Ticker extends Handler {
        private static class Task {
            View mView;
            Token mToken;
            public Task(View view, Token token) {
                mView = view;
                mToken = token;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            Task task = (Task) msg.obj;

            CircleProgressBar progress = (CircleProgressBar) task.mView.findViewById(R.id.progress);
            ImageView image = (ImageView) task.mView.findViewById(R.id.image);
            TextView code = (TextView) task.mView.findViewById(R.id.code);

            code.setText(task.mToken.getCode());

            int prog = task.mToken.getProgress();
            progress.setProgress(prog);
            if (prog > 0 && prog < 950)
                task.mView.setEnabled(true);

            if (task.mToken.getType() == TokenType.HOTP && prog == 0) {
                progress.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                return;
            }

            sendMessageDelayed(Message.obtain(msg), 100);
        }

        public void start(View view, Token token) {
            stop(view);

            Task task = new Task(view, token);
            Message msg = Message.obtain(this, System.identityHashCode(view), task);
            msg.sendToTarget();
        }

        public void stop(View view) {
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
        mTicker.stop(view);

        final CircleProgressBar progress = (CircleProgressBar) view.findViewById(R.id.progress);
        final ImageView image = (ImageView) view.findViewById(R.id.image);
        final TextView code = (TextView) view.findViewById(R.id.code);
        code.setText(token.getCode());

        // Update click listener
        View.OnClickListener ocl = null;
        if (token.getType() == TokenType.HOTP) {
            ocl = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    token.increment();
                    save(token);

                    ((TextView) v.findViewById(R.id.code)).setText(token.getCode());
                    progress.setVisibility(View.VISIBLE);
                    image.setVisibility(View.GONE);
                    v.setEnabled(false);

                    mTicker.start(v, token);
                }
            };
        }
        view.setOnClickListener(ocl);

        if (token.getType() == TokenType.TOTP) {
            view.setBackgroundResource(R.drawable.token_normal);
            progress.setVisibility(View.VISIBLE);
            image.setVisibility(View.GONE);
            mTicker.start(view, token);
        } else {
            view.setBackgroundResource(R.drawable.token);
            progress.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
        }
    }
}

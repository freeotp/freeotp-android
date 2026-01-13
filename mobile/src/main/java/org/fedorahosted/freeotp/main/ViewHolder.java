/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.text.BidiFormatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.Code;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;

import androidx.recyclerview.widget.RecyclerView;

class ViewHolder extends RecyclerView.ViewHolder {
    interface EventListener {
        boolean onSelectionToggled(ViewHolder holder);
        void onActivated(ViewHolder holder);
        void onShare(String code);
    }
    private static final String LOGTAG = "Adapter";

    private EventListener mEventListener;
    private ObjectAnimator mCountdown;
    private Handler mHandler;
    private BidiFormatter mBidiFormatter;

    private ProgressBar mProgress;
    private ImageButton mShare;
    private ViewGroup mPassive;
    private ViewGroup mActive;
    private ViewGroup mIcons;
    private ImageView mCheck;
    private ImageView mCheckActive;
    private ImageView mImage;
    private ImageView mImageActive;
    private ImageView mLock;
    private TextView mIssuer;
    private TextView mLabel;
    private TextView mCode;

    private View mView;

    private static final long SELECT_HALF_DURATION = 100; // milliseconds
    private static final long FADE_DURATION = 500; // milliseconds

    private final View.OnClickListener mSelectClick = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            v.animate()
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(SELECT_HALF_DURATION)
                .rotationY(90)
                .withLayer()
                .withEndAction(() -> {
                    setSelected(mEventListener.onSelectionToggled(ViewHolder.this));
                    v.setRotationY(-90);
                    v.animate()
                        .setInterpolator(new DecelerateInterpolator())
                        .setDuration(SELECT_HALF_DURATION)
                        .rotationY(0)
                        .withLayer()
                        .start();
                }).start();
        }
    };

    private static void fade(final View view, final boolean in, long duration) {
        view.setVisibility(View.VISIBLE);
        view.animate()
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setDuration(duration)
            .alpha(in ? 1f : 0f)
            .withLayer()
            .start();
    }

    private void displayCode(Code code, Token.Type type, long animationDuration) {
        if (code == null)
            return;

        String text = mBidiFormatter.unicodeWrap(code.getCode());
        Log.i(LOGTAG, "displaying Code");
        /* Add spaces for readability */
        for (int segment : new int[] { 7, 5, 4, 3 }) {
            if (text.length() % segment != 0)
                continue;

            int count = text.length() / segment;
            if (count < 2)
                break;

            StringBuilder sb = new StringBuilder();

            sb.append(" ");
            for (int i = 0; i < text.length(); i += segment) {
                if (i % 13 > (i + segment) % 13)
                    sb.append("\n ");

                sb.append(text.substring(i, i + segment));
                sb.append(" ");
            }

            text = sb.toString();
            break;
        }

        mCountdown.cancel();
        mCode.setText(text);
        long timeLeft = code.timeLeft();
        mCountdown.setDuration(timeLeft);

        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> fadeOut(), timeLeft);

        mCountdown.setIntValues(code.getProgress(mProgress.getMax()), 0);
        mCountdown.start();
    }

    void fadeOut() {
        /* Fade out */
        fade(mPassive, true, FADE_DURATION);
        fade(mActive, false, FADE_DURATION);

    }
    ViewHolder(View itemView, EventListener listener) {
        super(itemView);

        mBidiFormatter = BidiFormatter.getInstance();
        mEventListener = listener;
        mCountdown = new ObjectAnimator();
        mHandler = new Handler();
        mProgress = itemView.findViewById(R.id.progress_linear);
        mPassive = itemView.findViewById(R.id.passive);
        mActive = itemView.findViewById(R.id.active);
        mIssuer = itemView.findViewById(R.id.issuer);
        mLabel = itemView.findViewById(R.id.label);
        mIcons = itemView.findViewById(R.id.icons);
        mCheck = itemView.findViewById(R.id.check);
        mCheckActive = itemView.findViewById(R.id.check_active);
        mImage = itemView.findViewById(R.id.image);
        mImageActive = itemView.findViewById(R.id.image_active);
        mLock = itemView.findViewById(R.id.lock);
        mShare = itemView.findViewById(R.id.share);
        mCode = itemView.findViewById(R.id.code);
        mView = itemView;

        mCountdown.setInterpolator(new LinearInterpolator());
        mCountdown.setPropertyName("progress");
        mCountdown.setTarget(mProgress);
        mCountdown.setAutoCancel(true);
        mCountdown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mView.setEnabled(true);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                /* Fade in */
                Log.i(LOGTAG, "onAnimationStart: fade");
                fade(mPassive, false, FADE_DURATION);
                fade(mActive, true, FADE_DURATION);
            }
        });

        mIcons.setOnClickListener(mSelectClick);
        mImageActive.setOnClickListener(mSelectClick);

        mShare.setOnClickListener(v -> {
            String code = mCode.getText().toString();
            mEventListener.onShare(code.replaceAll("\\s+", ""));
        });

        mView.setOnClickListener(v -> {
            mEventListener.onActivated(ViewHolder.this);
        });

        mView.setOnLongClickListener(v -> {
            mSelectClick.onClick(mIcons);
            return true;
        });
    }

    private void setSelected(boolean selected) {
        mImage.setVisibility(selected ? View.INVISIBLE : View.VISIBLE);
        mImageActive.setVisibility(selected ? View.INVISIBLE : View.VISIBLE);
        mCheck.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        mCheckActive.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    void reset() {
        mCountdown.cancel();
    }

    void bind(Token token, int color, int image_id, String image_url, Code code, boolean selected, Token.Type type) {
        String issuer = token.getIssuer();
        if (issuer == null)
            issuer = mView.getResources().getString(R.string.unknown_issuer);

        mLock.setVisibility(token.getLock() ? View.VISIBLE : View.GONE);
        mIssuer.setText(issuer);
        mLabel.setText(token.getLabel());
        mImage.setBackgroundColor(color);
        mImageActive.setBackgroundColor(color);

        if (image_url == null || image_url.isEmpty()) {
            mImage.setImageResource(image_id);
            mImageActive.setImageResource(image_id);
        } else {
            Picasso.get().load(image_url).error(image_id).into(mImage);
            Picasso.get().load(image_url).error(image_id).into(mImageActive);
        }

        setSelected(selected);
        if (code != null) {
            displayCode(code, type, 0);
        }
    }

    void displayCode(Code code, Token.Type type) {
        displayCode(code, type, FADE_DURATION);
    }

    CharSequence getIssuer() {
        return mIssuer.getText();
    }

    CharSequence getLabel() {
        return mLabel.getText();
    }
}

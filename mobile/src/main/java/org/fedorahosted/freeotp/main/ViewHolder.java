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
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
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

import java.util.Locale;

class ViewHolder extends RecyclerView.ViewHolder {
    interface EventListener {
        boolean onSelectionToggled(ViewHolder holder);
        void onActivated(ViewHolder holder);
        void onShare(String code);
    }

    private EventListener mEventListener;
    private ObjectAnimator mCountdown;

    private ProgressBar mProgress;
    private ImageButton mShare;
    private ViewGroup mPassive;
    private ViewGroup mActive;
    private ViewGroup mIcons;
    private ImageView mCheck;
    private ImageView mImage;
    private ImageView mLock;
    private TextView mIssuer;
    private TextView mLabel;
    private TextView mCode;
    private View mView;

    private final View.OnClickListener mViewClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            mEventListener.onActivated(ViewHolder.this);
        }
    };

    private final View.OnClickListener mShareClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String code = mCode.getText().toString();
            mEventListener.onShare(code.replaceAll("\\s+", ""));
        }
    };

    private final View.OnClickListener mSelectClick = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            v.animate()
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(100)
                .rotationY(90)
                .withLayer()
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        int pos = getAdapterPosition();
                        setSelected(mEventListener.onSelectionToggled(ViewHolder.this));
                        v.setRotationY(-90);
                        v.animate()
                            .setInterpolator(new DecelerateInterpolator())
                            .setDuration(100)
                            .rotationY(0)
                            .withLayer()
                            .start();
                    }
                }).start();
        }
    };

    private static void fade(final View view, final boolean in, int duration) {
        view.setVisibility(View.VISIBLE);
        view.animate()
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setDuration(duration)
            .alpha(in ? 1f : 0f)
            .withLayer()
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    if (in)
                        view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!in)
                        view.setVisibility(View.GONE);
                }
            })
            .start();
    }

    private void displayCode(Code code, int animationDuration) {
        if (code == null)
            return;

        String text = code.getCode();
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

        mView.setEnabled(false);
        fade(mPassive, false, animationDuration);
        fade(mActive, true, animationDuration);

        mCode.setText(text);
        mCountdown.setDuration(code.timeLeft());
        mCountdown.setIntValues(code.getProgress(mProgress.getMax()), 0);
        mCountdown.start();
    }

    ViewHolder(View itemView, EventListener listener) {
        super(itemView);

        mEventListener = listener;
        mCountdown = new ObjectAnimator();
        mProgress = itemView.findViewById(R.id.progress);
        mPassive = itemView.findViewById(R.id.passive);
        mActive = itemView.findViewById(R.id.active);
        mIssuer = itemView.findViewById(R.id.issuer);
        mLabel = itemView.findViewById(R.id.label);
        mIcons = itemView.findViewById(R.id.icons);
        mCheck = itemView.findViewById(R.id.check);
        mImage = itemView.findViewById(R.id.image);
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
                mPassive.clearAnimation();
                mActive.clearAnimation();
                mView.setEnabled(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                fade(mPassive, true, 500);
                fade(mActive, false, 500);
                mView.setEnabled(true);
            }
        });

        mIcons.setOnClickListener(mSelectClick);
        mShare.setOnClickListener(mShareClick);
        mView.setOnClickListener(mViewClick);
    }

    private int getIdentifier(String type, String prefix, Token token) {
        String issuer = token.getIssuer();
        if (issuer == null)
            return 0;

        String name = prefix + issuer.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_");
        String pkg = mView.getContext().getPackageName();

        return mView.getResources().getIdentifier(name, type, pkg);
    }

    private void setBackgroundColor(Token token) {
        try {
            // If the token specified a color, use it.
            mImage.setBackgroundColor(Color.parseColor("#" + token.getColor()));
        } catch (NumberFormatException e) {
            Resources r = mView.getResources();
            try {
                // If the token didn't specify a color but we know the Issuer's color, use it.
                mImage.setBackgroundColor(r.getColor(getIdentifier("color", "brand_", token)));
            } catch (Resources.NotFoundException f) {
                // Otherwise, pick a color that will be constant for the same Issuer string.
                int[] backgrounds = r.getIntArray(R.array.backgrounds);
                int idx = 0;

                try { idx = Math.abs(token.getIssuer().hashCode()); }
                catch (NullPointerException g) { }

                mImage.setBackgroundColor(backgrounds[idx % backgrounds.length]);
            }
        }
    }

    private void setImage(Token token) {
        int id = getIdentifier("drawable", "fa_", token);
        if (id == 0) {
            switch (token.getType()) {
                case HOTP: id = R.drawable.ic_hotp; break;
                case TOTP: id = R.drawable.ic_totp; break;
            }
        }

        String url = token.getImage();
        if (url == null) {
            mImage.setImageResource(id);
        } else {
            Picasso.get().load(url).error(id).into(mImage);
        }
    }

    private void setSelected(boolean selected) {
        mImage.setVisibility(selected ? View.INVISIBLE : View.VISIBLE);
        mCheck.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    void reset() {
        mCountdown.cancel();
    }

    void bind(Token token, Code code, boolean selected) {
        reset();

        String issuer = token.getIssuer();
        if (issuer == null)
            issuer = mView.getResources().getString(R.string.unknown_issuer);

        mLock.setVisibility(token.getLock() ? View.VISIBLE : View.GONE);
        mIssuer.setText(issuer);
        mLabel.setText(token.getLabel());
        setBackgroundColor(token);
        setImage(token);

        setSelected(selected);
        if (code != null)
            displayCode(code, 0);
    }

    void displayCode(Code code) {
        displayCode(code, 500);
    }

    CharSequence getIssuer() {
        return mIssuer.getText();
    }

    CharSequence getLabel() {
        return mLabel.getText();
    }
}

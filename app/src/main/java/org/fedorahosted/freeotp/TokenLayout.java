package org.fedorahosted.freeotp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.edit.DeleteActivity;
import org.fedorahosted.freeotp.edit.EditActivity;

public class TokenLayout extends FrameLayout implements View.OnClickListener, PopupMenu.OnMenuItemClickListener, Runnable {
    private ProgressCircle mProgressInner;
    private ProgressCircle mProgressOuter;
    private ImageView mImage;
    private TextView mCode;
    private TextView mIssuer;
    private TextView mLabel;
    private ImageView mMenu;
    private PopupMenu mPopupMenu;

    private TokenCode mCodes;
    private int mPosition;
    private Token mToken;
    private long mStartTime;

    public TokenLayout(Context context) {
        super(context);
    }

    public TokenLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TokenLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mProgressInner = (ProgressCircle) findViewById(R.id.progressInner);
        mProgressOuter = (ProgressCircle) findViewById(R.id.progressOuter);
        mImage = (ImageView) findViewById(R.id.image);
        mCode = (TextView) findViewById(R.id.code);
        mIssuer = (TextView) findViewById(R.id.issuer);
        mLabel = (TextView) findViewById(R.id.label);
        mMenu = (ImageView) findViewById(R.id.menu);

        setOnClickListener(this);
        mMenu.setOnClickListener(this);

        mPopupMenu = new PopupMenu(getContext(), mMenu);
        mPopupMenu.getMenuInflater().inflate(R.menu.token, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(this);
    }

    public void setToken(int position, Token token) {
        mPosition = position;
        mToken = token;
        mCodes = null;

        // Cancel all active animations.
        setEnabled(true);
        removeCallbacks(this);
        mImage.clearAnimation();
        mProgressInner.clearAnimation();
        mProgressOuter.clearAnimation();
        mProgressInner.setVisibility(View.GONE);
        mProgressOuter.setVisibility(View.GONE);

        // Show the image.
        Picasso.with(getContext())
                .load(mToken.getImage())
                .placeholder(R.drawable.logo)
                .into(mImage);

        // Set the labels.
        mLabel.setText(mToken.getLabel());
        mIssuer.setText(mToken.getIssuer());
        if (mIssuer.getText().length() == 0) {
            mIssuer.setText(mToken.getLabel());
            mLabel.setVisibility(View.GONE);
        } else {
            mLabel.setVisibility(View.VISIBLE);
        }

        // Set the code placeholder.
        char[] placeholder = new char[mToken.getDigits()];
        for (int i = 0; i < placeholder.length; i++)
            placeholder[i] = '-';
        mCode.setText(new String(placeholder));
    }

    @Override
    public void onClick(View v) {
        if (v == mMenu) {
            mPopupMenu.show();
            return;
        }

        // Increment the token.
        mCodes = mToken.generateCodes();
        new TokenPersistence(getContext()).save(mToken);

        // Copy code to clipboard.
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(null, mCodes.getCurrentCode()));

        // Start animations.
        mProgressInner.setVisibility(View.VISIBLE);
        mProgressInner.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fadein));
        mImage.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.token_image_fadeout));

        // Handle type-specific UI.
        switch (mToken.getType()) {
            case HOTP:
                setEnabled(false);
                break;
            case TOTP:
                mProgressOuter.setVisibility(View.VISIBLE);
                mProgressOuter.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fadein));
                break;
        }

        mStartTime = System.currentTimeMillis();
        post(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent i;

        if (mToken == null)
            return false;

        switch (item.getItemId()) {
            case R.id.action_edit:
                i = new Intent(getContext(), EditActivity.class);
                i.putExtra(EditActivity.EXTRA_POSITION, mPosition);
                getContext().startActivity(i);
                break;

            case R.id.action_delete:
                i = new Intent(getContext(), DeleteActivity.class);
                i.putExtra(DeleteActivity.EXTRA_POSITION, mPosition);
                getContext().startActivity(i);
                break;
        }

        return true;
    }

    @Override
    public void run() {
        // Get the current data
        String code = mCodes.getCurrentCode();
        if (code != null) {
            // Determine whether to enable/disable the view.
            if (!isEnabled())
                setEnabled(System.currentTimeMillis() - mStartTime > 5000);

            // Update the fields
            mCode.setText(code);
            mProgressInner.setProgress(mCodes.getCurrentProgress());
            if (mToken.getType() != Token.TokenType.HOTP)
                mProgressOuter.setProgress(mCodes.getTotalProgress());

            postDelayed(this, 100);
            return;
        }

        setToken(mPosition, mToken);
        mImage.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.token_image_fadein));
    }
}

package net.sagaoftherealms.wearauthenticator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import java.util.List;

public class ManageTokens extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ManageTokens";
    private GridViewPager pager;
    private TokenPersistence persistence;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pager.invalidate();
                    pager.getAdapter().notifyDataSetChanged();
                    pager.setCurrentItem(0, 0);//For now if data changes reset to origin

                }
            });

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_tokens);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        persistence = new TokenPersistence(getApplicationContext());
        pager = (GridViewPager) findViewById(R.id.pager);
        pager.setBackgroundResource(R.drawable.logo);
        pager.setAdapter(new GridPagerAdapter() {
            @Override
            public int getRowCount() {
                if (persistence.length() == 0) {
                    return 1;
                }
                return persistence.length();
            }

            @Override
            public int getColumnCount(int i) {
                if (persistence.length() == 0) {
                    return 1;
                } else {
                    return 1;
                }
            }



            @Override
            protected Object instantiateItem(ViewGroup viewGroup, int row, int col) {
                final View view;
                if (persistence.length() == 0) {
                    view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.no_tokens, viewGroup, false);
                } else {
                    final Token token = persistence.get(row);
                    switch (col) {
                        case 0:
                            view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.rect_activity_fetch_auth_token, viewGroup, false);
                            ((TextView) view.findViewById(R.id.issuer)).setText(token.getIssuer());
                            ((TextView) view.findViewById(R.id.label)).setText(token.getLabel());
                            view.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ((TextView) view.findViewById(R.id.token_text)).setText(token.generateCodes().getCurrentCode());
                                    persistence.sync(token, mGoogleApiClient);
                                }
                            });
                            break;
                        case 1:
                            view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.make_token_default, viewGroup, false);
                            if (token.getID().equals(persistence.getDefaultTokenId())) {
                                (view.findViewById(R.id.button)).setVisibility(View.GONE);
                                ((TextView) view.findViewById(R.id.label)).setText("Shown when launched by voice.");
                                view.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        persistence.setDefaultTokenId(token.getID());
                                        (view.findViewById(R.id.button)).setVisibility(View.GONE);
                                        ((TextView) view.findViewById(R.id.label)).setText("Shown when launched by voice.");
                                    }
                                });
                            }
                            break;
                        case 2:
                            view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.edit_on_phone, viewGroup, false);
                            break;
                        default:
                            throw new IllegalStateException("There are three view");
                    }

                }
                viewGroup.addView(view);
                return view;
            }

            @Override
            protected void destroyItem(ViewGroup viewGroup, int row, int col, Object o) {
                viewGroup.removeView((View) o);
            }

            @Override
            public boolean isViewFromObject(View view, Object o) {
                return view == o;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(dataUpdateReceiver, IntentFilter.create("newData", "vnd.android.wear/token"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(dataUpdateReceiver);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Suspended Connection to Google Api Service");
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection to Google Api Service Failed");
    }

}


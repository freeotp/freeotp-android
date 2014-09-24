package net.sagaoftherealms.wearauthenticator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
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


public class FetchAuthToken extends Activity implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = "WearActivity";
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetch_auth_token);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.token_text);
                TokenPersistence persistence = new TokenPersistence(getApplicationContext());
                if (persistence.length() > 0) {
                    Token token = persistence.get(0);
                    mTextView.setText(token.generateCodes().getCurrentCode());
                } else {
                    mTextView.setText("--- ---");
                }

            }
        });
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEvents) {
        final TokenPersistence persistence = new TokenPersistence(getApplicationContext());
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {



                if (persistence.length() > 0) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(events.get(0).getDataItem());
                    String tokenJson = dataMapItem.getDataMap().getString(TokenPersistence.TOKEN_KEY);
                    Token token = new Gson().fromJson(tokenJson, Token.class);
                    mTextView.setText(token.generateCodes().getCurrentCode());
                    dataEvents.release();
                } else {
                    mTextView.setText("--- ---");
                }
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
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Suspended Connection to Google Api Service");
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection to Google Api Service Failed");
    }
}

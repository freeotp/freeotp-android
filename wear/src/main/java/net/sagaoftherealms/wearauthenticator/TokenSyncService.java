package net.sagaoftherealms.wearauthenticator;

import android.os.Binder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

/**
 * Created by Summers on 9/22/2014.
 */
public class TokenSyncService extends WearableListenerService {

    private static final String TAG = "DataLayerSample";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);

        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : events) {

            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());


            String tokenJson = dataMapItem.getDataMap().getString(TokenPersistence.TOKEN_KEY);
            Token token = new Gson().fromJson(tokenJson, Token.class);

            long binderToken = Binder.clearCallingIdentity();
            try {
                TokenPersistence tokenPersistence = new TokenPersistence(getApplicationContext());
                if (tokenPersistence.length() > 1) {
                    tokenPersistence.delete(0);
                }
                tokenPersistence.add(token);
            } catch (Token.TokenUriInvalidException e) {
                Log.e("Sync service",e.getMessage(), e);
            } finally {
                Binder.restoreCallingIdentity(binderToken);
            }



        }
        googleApiClient.disconnect();
    }

}

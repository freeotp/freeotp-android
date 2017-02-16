package org.fedorahosted.freeotp.wear;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;

import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Summers on 9/22/2014.
 */
public class TokenSyncService extends WearableListenerService {

    private static final String TAG = "DataLayerSample";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        Log.d(TAG, "onPeerConnected: " + peer.getDisplayName());
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
        Log.d(TAG, "onPeerDISConnected: " + peer.getDisplayName());
    }

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

        Node localNode = Wearable.NodeApi.getLocalNode(googleApiClient).await().getNode();

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : events) {

            if (event.getDataItem().getUri().getHost().contains(localNode.getId())) {
                continue;
            }


            TokenPersistence tokenPersistence = new TokenPersistence(this);
            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
            DataMap dataMap = dataMapItem.getDataMap();


            for (String key : dataMap.keySet()) {
                try {
                    Token token = new Gson().fromJson(dataMap.getString(key), Token.class);
                    tokenPersistence.save(token);
                } catch (Exception ignore) {
                    Log.e(TAG, ignore.getMessage(), ignore);
                }
            }

        }
        googleApiClient.disconnect();
    }

}

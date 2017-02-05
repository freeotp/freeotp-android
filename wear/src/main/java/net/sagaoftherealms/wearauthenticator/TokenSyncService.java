package net.sagaoftherealms.wearauthenticator;

import android.content.Intent;
import android.os.Binder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
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
    private String node = "";

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
        Log.d("PEER", "DisConnected");
        node = "";
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        Log.d("PEER", "Connected");
        node = peer.getId();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);
        boolean announce = false;
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
            announce = true;
            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
            TokenPersistence tokenPersistence = new TokenPersistence(getApplicationContext());
            if (event.getDataItem().getUri().getPath().contains("tokens")) {
                DataMap dataMap = dataMapItem.getDataMap();
                tokenPersistence.clear();
                for (String key : dataMap.keySet()) {
                    if (key.equals(TokenPersistence.ORDER) || key.equals("time")) {
                        continue;
                    }
                    String tokenJson = dataMap.getString(key);
                    Token token = new Gson().fromJson(tokenJson, Token.class);
                    try {
                        tokenPersistence.add(token);
                    } catch (Token.TokenUriInvalidException e) {
                        Log.e("Sync service", e.getMessage(), e);
                    }
                }
                String order = dataMapItem.getDataMap().getString(TokenPersistence.ORDER);
                tokenPersistence.setOrder(order);
            }
            if (announce) {
                sendBroadcast(new Intent("newData").setType("vnd.android.wear/token"));
            }


        }
        googleApiClient.disconnect();
    }

}

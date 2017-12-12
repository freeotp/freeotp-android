package org.fedorahosted.freeotp.share;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.fedorahosted.freeotp.BaseTokenActivity;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

public class ShareActivity extends BaseTokenActivity implements Discoverable.DiscoveryCallback {
    private final Adapter mShareTokenAdapter = new Adapter();
    private Discoverable[] mDiscoverables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share);

        mDiscoverables = new Discoverable[] {
        };

        RecyclerView rv = findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv.setAdapter(mShareTokenAdapter);
        rv.setHasFixedSize(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        for (Discoverable d : mDiscoverables) {
            if (d.permissions().length == 0)
                onRequestPermissionsResult(0, d.permissions(), new int[0]);
            else if (d.permitted())
                requestPermissions(d.permissions(), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] results) {
        for (Discoverable d : mDiscoverables) {
            if (!d.permitted() || d.isDiscovering())
                continue;

            Intent intent = d.enablement();
            if (intent != null)
                startActivityForResult(intent, 0);
            else
                onActivityResult(0, 0, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (Discoverable d : mDiscoverables) {
            if (!d.permitted() || d.isDiscovering())
                continue;

            Intent intent = d.enablement();
            if (intent == null)
                d.startDiscovery();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        for (Discoverable d : mDiscoverables) {
            if (d.isDiscovering())
                d.stopDiscovery();
        }
    }

    @Override
    public void onShareAppeared(final Discoverable discoverable, final Adapter.Item item,
                                final Discoverable.Shareable shareable) {
        if (shareable == null) {
            item.setOnClickListener(new Adapter.Item.OnClickListener() {
                @Override
                public void onClick(Adapter.Item item) {
                    requestPermissions(discoverable.permissions(), 0);
                }
            });
        } else {
            item.setOnClickListener(new Adapter.Item.OnClickListener() {
                @Override
                public void onClick(Adapter.Item item) {
                    TokenPersistence tp = new TokenPersistence(ShareActivity.this);
                    Token t = tp.get(getPosition());
                    String tc = t.generateCodes().getCurrentCode();
                    tp.save(t);

                    item.setOnClickListener(null);
                    for (int i = 0; i < mShareTokenAdapter.getItemCount(); i++)
                        mShareTokenAdapter.get(i).setEnabled(false);

                    shareable.share(tc, new Discoverable.Shareable.ShareCallback() {
                        @Override
                        public void onShareCompleted(boolean success) {
                            finish();
                        }
                    });
                }
            });
        }

        mShareTokenAdapter.add(item);
    }

    @Override
    public void onShareDisappeared(Discoverable discoverable, final Adapter.Item item) {
        mShareTokenAdapter.remove(item);
    }
}

package org.fedorahosted.freeotp.edit;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class DeleteActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.delete);

        final Token token = new TokenPersistence(this).get(getPosition());
        ((TextView) findViewById(R.id.issuer)).setText(token.getIssuer());
        ((TextView) findViewById(R.id.label)).setText(token.getLabel());
        Picasso.with(this)
                .load(token.getImage())
                .placeholder(R.mipmap.ic_freeotp_logo_foreground)
                .into((ImageView) findViewById(R.id.image));

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //delete the image that was copied to storage, before deleting the token
                token.deleteImage();
                new TokenPersistence(DeleteActivity.this).delete(getPosition());
                finish();
            }
        });
    }
}

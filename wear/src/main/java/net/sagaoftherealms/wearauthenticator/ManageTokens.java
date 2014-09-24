package net.sagaoftherealms.wearauthenticator;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenPersistence;

public class ManageTokens extends Activity {

    private TextView mTextView;
    private GridViewPager pager;
    private TokenPersistence persistence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_tokens);
        persistence = new TokenPersistence(getApplicationContext());
        pager = (GridViewPager) findViewById(R.id.pager);

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
                    return 3;
                }
            }

            @Override
            protected Object instantiateItem(ViewGroup viewGroup, int row, int col) {
                View view;
                if (getColumnCount(0) == 1) {
                    view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.no_tokens, viewGroup, false);
                } else {
                    Token token = persistence.get(row);
                    switch (col) {
                        case 1:
                        view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.no_tokens, viewGroup, false);
                        break;
                        case 2:
                        view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.no_tokens, viewGroup, false);
                        break;
                        case 3:
                        view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.no_tokens, viewGroup, false);
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
}

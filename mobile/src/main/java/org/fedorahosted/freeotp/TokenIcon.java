/*
 * FreeOTP
 *
 * Authors: Justin Stephenson <jstephen@redhat.com>
 *
 * Copyright (C) 2022  Justin Stephenson, Red Hat
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

package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Pair;

import java.util.Locale;

public class TokenIcon {
    public Pair<Integer, String> mImage;
    public int mColor;
    private Context mContext;

    public TokenIcon(Token token, Context context) {
        mContext = context;
        mImage = getImage(token);
        mColor = getBackgroundColor(token);
    }

    private int getIdentifier(String type, String prefix, Token token) {
        String issuer = token.getIssuer();
        if (issuer == null)
            return 0;

        /* Remove spaces */
        issuer = issuer.replaceAll(" ", "");

        String name = prefix + issuer.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_");
        String pkg = mContext.getPackageName();

        return mContext.getResources().getIdentifier(name, type, pkg);
    }

    private int getBackgroundColor(Token token) {
        int color;
        try {
            // If the token specified a color, use it.
            color = Color.parseColor("#" + token.getColor());
        } catch (NumberFormatException e) {
            Resources r = mContext.getResources();
            try {
                // If the token didn't specify a color but we know the Issuer's color, use it.
                color = r.getColor(getIdentifier("color", "brand_", token));
            } catch (Resources.NotFoundException f) {
                // Otherwise, pick a color that will be constant for the same Issuer string.
                int[] backgrounds = r.getIntArray(R.array.backgrounds);
                int idx = 0;

                try { idx = Math.abs(token.getIssuer().hashCode()); }
                catch (NullPointerException g) { }

                color = backgrounds[idx % backgrounds.length];
            }
        }

        return color;
    }

    private Pair<Integer, String> getImage(Token token) {
        String url = token.getImage();
        int id = 0;
        id = getIdentifier("drawable", "fa_", token);
        if (id == 0) {
            switch (token.getType()) {
                case HOTP: id = R.drawable.ic_hotp; break;
                case TOTP: id = R.drawable.ic_totp; break;
            }
        }

        return new Pair<Integer, String>(id, url);
    }
}

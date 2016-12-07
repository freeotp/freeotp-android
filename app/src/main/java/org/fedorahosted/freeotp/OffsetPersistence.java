/*
 * FreeOTP
 *
 * Authors: Christian Mäder <mail a cimnine ch>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
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
import android.content.SharedPreferences;

/**
 * Keeps record of the defined offset
 */

class OffsetPersistence {
    private static final String NAME = "offset";
    private final SharedPreferences prefs;

    OffsetPersistence(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    int getInSeconds() {
        return prefs.getInt(NAME, 0);
    }

    long getInMillis() {
        return (long)getInSeconds() * 1000L;
    }

    void store(int offsetInSeconds) {
        prefs.edit().putInt(NAME, offsetInSeconds).apply();
    }
}

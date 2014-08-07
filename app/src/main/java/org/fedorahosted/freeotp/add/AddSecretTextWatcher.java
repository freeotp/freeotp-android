/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
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

package org.fedorahosted.freeotp.add;

import android.app.Activity;
import android.text.Editable;

public class AddSecretTextWatcher extends AddTextWatcher {
    public AddSecretTextWatcher(Activity activity) {
        super(activity);
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() != 0) {
            // Ensure that = is only permitted at the end
            boolean haveData = false;
            for (int i = s.length() - 1; i >= 0; i--) {
                char c = s.charAt(i);
                if (c != '=')
                    haveData = true;
                else if (haveData)
                    s.delete(i, i + 1);
            }
        }

        super.afterTextChanged(s);
    }
}

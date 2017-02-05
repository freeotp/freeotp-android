/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2014  Nathaniel McCallum, Red Hat
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

public class TokenCode {
    private final String mCode;
    private final long mStart;
    private final long mUntil;
    private TokenCode mNext;

    public TokenCode(String code, long start, long until) {
        mCode = code;
        mStart = start;
        mUntil = until;
    }

    public TokenCode(TokenCode prev, String code, long start, long until) {
        this(code, start, until);
        prev.mNext = this;
    }

    public TokenCode(String code, long start, long until, TokenCode next) {
        this(code, start, until);
        mNext = next;
    }

    public String getCurrentCode() {
        TokenCode active = getActive(System.currentTimeMillis());
        if (active == null)
            return null;
        return active.mCode;
    }

    public int getTotalProgress() {
        long cur = System.currentTimeMillis();
        long total = getLast().mUntil - mStart;
        long state = total - (cur - mStart);
        return (int) (state * 1000 / total);
    }

    public int getCurrentProgress() {
        long cur = System.currentTimeMillis();
        TokenCode active = getActive(cur);
        if (active == null)
            return 0;

        long total = active.mUntil - active.mStart;
        long state = total - (cur - active.mStart);
        return (int) (state * 1000 / total);
    }

    private TokenCode getActive(long curTime) {
        if (curTime >= mStart && curTime < mUntil)
            return this;

        if (mNext == null)
            return null;

        return this.mNext.getActive(curTime);
    }

    private TokenCode getLast() {
        if (mNext == null)
            return this;
        return this.mNext.getLast();
    }
}

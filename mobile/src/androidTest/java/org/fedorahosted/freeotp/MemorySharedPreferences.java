package org.fedorahosted.freeotp;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemorySharedPreferences implements SharedPreferences {
    private static abstract class Editor implements SharedPreferences.Editor {
        static class Instruction {
            final boolean put;
            final String key;
            final Object val;

            Instruction() {
                put = false;
                val = null;
                key = null;
            }

            Instruction(String key) {
                this.put = false;
                this.val = null;
                this.key = key;
            }

            Instruction(String key, Object val) {
                this.put = true;
                this.val = val;
                this.key = key;
            }
        }

        List<Editor.Instruction> mInstructions = new LinkedList<>();

        @Override
        public SharedPreferences.Editor putString(String key, @Nullable String value) {
            mInstructions.add(new Editor.Instruction(key, value));
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
            mInstructions.add(new Editor.Instruction(key, values));
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            mInstructions.add(new Editor.Instruction(key, value));
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            mInstructions.add(new Editor.Instruction(key, value));
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            mInstructions.add(new Editor.Instruction(key, value));
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            mInstructions.add(new Editor.Instruction(key, value));
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            mInstructions.add(new Editor.Instruction(key));
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            mInstructions.add(new Editor.Instruction());
            return this;
        }

        @Override
        public boolean commit() {
            apply();
            return true;
        }
    }

    private Set<OnSharedPreferenceChangeListener> mListeners = new HashSet<>();
    private Map<String, Object> mPreferences = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
        return mPreferences;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        if (!mPreferences.containsKey(key))
            return defValue;
        return (String) mPreferences.get(key);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        if (!mPreferences.containsKey(key))
            return defValues;
        return (Set<String>) mPreferences.get(key);
    }

    @Override
    public int getInt(String key, int defValue) {
        if (!mPreferences.containsKey(key))
            return defValue;
        return (Integer) mPreferences.get(key);
    }

    @Override
    public long getLong(String key, long defValue) {
        if (!mPreferences.containsKey(key))
            return defValue;
        return (Long) mPreferences.get(key);
    }

    @Override
    public float getFloat(String key, float defValue) {
        if (!mPreferences.containsKey(key))
            return defValue;
        return (Float) mPreferences.get(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (!mPreferences.containsKey(key))
            return defValue;
        return (Boolean) mPreferences.get(key);
    }

    @Override
    public boolean contains(String key) {
        return mPreferences.containsKey(key);
    }

    private void onChange(String key) {
        for (OnSharedPreferenceChangeListener l : mListeners)
            l.onSharedPreferenceChanged(this, key);
    }

    @Override
    public SharedPreferences.Editor edit() {
        return new Editor() {
            @Override
            public void apply() {
                for (Iterator<Instruction> iitr = mInstructions.iterator(); iitr.hasNext(); ) {
                    Instruction i = iitr.next();
                    if (i.key != null) {
                        if (i.put)
                            mPreferences.put(i.key, i.val);
                        else
                            mPreferences.remove(i.key);

                        onChange(i.key);
                        continue;
                    }

                    for (Iterator<String> sitr = mPreferences.keySet().iterator(); sitr.hasNext(); ) {
                        onChange(sitr.next());
                        sitr.remove();
                    }

                    iitr.remove();
                }
            }
        };
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mListeners.remove(listener);
    }
}

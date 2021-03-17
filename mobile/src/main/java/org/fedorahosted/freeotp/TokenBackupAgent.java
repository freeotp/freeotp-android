package org.fedorahosted.freeotp;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

public class TokenBackupAgent extends BackupAgentHelper{
        static final String BACKUP = "tokenBackup";
        static final String RESTORED = "restoreComplete";
        private SharedPreferences mBackups;
        static final String PREFS_BACKUP_KEY = "data";

        @Override
        public void onCreate() {
            super.onCreate();
            SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, BACKUP);
            addHelper(PREFS_BACKUP_KEY, helper);
        }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        super.onBackup(oldState, data, newState);
    }

    @Override
    public void onFullBackup(FullBackupDataOutput data) throws IOException {
        super.onFullBackup(data);
        mBackups = getSharedPreferences(BACKUP, Context.MODE_PRIVATE);
    }

    @Override
        public void onRestoreFinished() {
            super.onRestoreFinished();
            mBackups = getSharedPreferences(BACKUP, Context.MODE_PRIVATE);
            mBackups.edit().putBoolean(RESTORED, true).apply();
        }
    }

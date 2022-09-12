# Backup and Restore

The FreeOTP 2.0+ release migrates token secrets into the Android Keystore.
This is a one-way operation which makes token secrets impossible to extract, by design.
On upgrade, users will be required to setup a master password to be stored in the Android Keystore and used to encrypt token data. Restore operations will always prompt the user for this master password.

FreeOTP supports two different backup and restore methods:

  1. Back up token data to Google Account (Android Auto backup)
  2. in-app functionality for external storage backup and restore (Manual)

In both cases, token data is stored in a shared preferences `.xml` file, encrypted with the master key stored in the Android Keystore.

## Google Backup

FreeOTP opts in to participate in the Google-preferred way to backup and restore data in an automated way. The following link
[Back up or restore data on your Android device](https://support.google.com/android/answer/2819582?hl=en)
contains instructions on how to enable this backup method.

Once backup data is available, restored token data will be pushed to the device on install of the FreeOTP app. FreeOTP will detect
this and prompt users for their master password to execute the restore operation.

## External (Manual) Backup

Inside of FreeOTP, the top toolbar menu contains items labeled 'Backup' and 'Restore'. Choosing `Backup` will prompt to select a path where to save the encrypted `.xml` file to. Restoring will prompt to select which `.xml` file to
restore from. It is the responsibility of users to copy this `.xml` file remotely, or backup to some other location if the device external storage is lost or inaccessible.
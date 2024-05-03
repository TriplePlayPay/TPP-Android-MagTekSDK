package com.tripleplaypay.magteksdk;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MagTekBLESupport {
    static final String TAG = MagTekBLESupport.class.getSimpleName();

    final MagTekBLEController bleController;

    MagTekBLESupport(MagTekBLEController bleController) {
        this.bleController = bleController;
    }

    private boolean missingBluetoothConnecting(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return missingBluetoothPermission(activity, Manifest.permission.BLUETOOTH_CONNECT, 2);
        else
            return false;
    }

    private boolean missingBluetoothScanning(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return missingBluetoothPermission(activity, Manifest.permission.BLUETOOTH_SCAN, 1);
        else
            return false;
    }

    private boolean missingBluetoothPermission(Activity activity, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[] {permission}, requestCode);
            return true;
        }
        return false;
    }
}

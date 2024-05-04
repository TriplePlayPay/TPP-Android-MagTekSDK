package com.tripleplaypay.magteksdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Hashtable;

public class MagTekBLESupport {
    static final String TAG = MagTekBLESupport.class.getSimpleName();

    Hashtable<String, BluetoothDevice> devices = new Hashtable<>();
    DeviceDiscoveredCallback deviceDiscoveredCallback;
    Activity activity;

    final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (missingBluetoothConnecting(activity))
                return;

            BluetoothDevice device = result.getDevice();
            String name = device.getName();

            // if the name is visible + the name has tDynamo in it somewhere + it has not been
            // discovered yet
            if (name != null && name.contains("tDynamo") && !devices.containsKey(name)) {
                Log.i(TAG, String.format("found device: %s: %d", name, result.getRssi()));
                devices.put(name, device);
                if (deviceDiscoveredCallback != null)
                    deviceDiscoveredCallback.callback(name, result.getRssi());
            }
        }
    };

    MagTekBLESupport(Activity activity) {
        this.activity = activity;
    }

    public String getDeviceAddress(String name) {
        BluetoothDevice device = devices.get(name);
        if (device != null)
            return device.getAddress();
        return ""; // silently fail, no need to crash
    }

    @SuppressLint("MissingPermission")
    public void startScanningForPeripherals(DeviceDiscoveredCallback deviceDiscoveredCallback) {
        this.deviceDiscoveredCallback = deviceDiscoveredCallback;
        if (missingBluetoothScanning(activity))
            return;
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothManager.getAdapter().getBluetoothLeScanner().startScan(scanCallback);
    }

    @SuppressLint("MissingPermission")
    public void stopScanningForPeripherals() {
        this.deviceDiscoveredCallback = null;
        if (missingBluetoothScanning(activity))
            return;
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothManager.getAdapter().getBluetoothLeScanner().stopScan(scanCallback);
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

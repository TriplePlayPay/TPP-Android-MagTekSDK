package com.tripleplaypay.magteksdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Hashtable;

public class MagTekBLEController {
    static final String TAG = MagTekBLEController.class.getSimpleName();

    final Activity activity;
    final BluetoothLeScanner leScanner;

    DeviceDiscoveredCallback deviceDiscoveredCallback;
    boolean scanning = false;

    final Hashtable<String, BluetoothDevice> devices = new Hashtable<>();

    final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 2);
            return; // if the permission was not set they will have to click on their button again
        }

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

    MagTekBLEController(Activity activity) {
        // MagTek tDynamo is a Bluetooth LE device. Therefore we need to access the BLE
        // functionality of Android's bluetooth adapter.

        // get handle to bluetooth adapter subsystem ->
        this.leScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        this.activity = activity;
    }

    public String getDeviceAddress(String name) {
        return this.devices.get(name).getAddress();
    }

    public boolean isScanning() {
        return this.scanning;
    }

    public void toggleScan(DeviceDiscoveredCallback deviceDiscoveredCallback) {
        this.deviceDiscoveredCallback = deviceDiscoveredCallback;

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.BLUETOOTH_SCAN }, 1);
            return; // if the permission was not set they will have to click on their button again
        }

        if (!this.scanning) {
            Log.i(this.TAG, "begin LE scan");
            this.scanning = true;
            this.leScanner.startScan(scanCallback);
        } else {
            Log.i(this.TAG, "end LE scan");
            this.scanning = false;
            this.leScanner.stopScan(scanCallback);
        }
    }
}

package com.tripleplaypay.magteksdk;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRA;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.magtek.mobile.android.mtlib.MTSCRAEvent.OnDeviceConnectionStateChanged;



public class MagTekCardReader {
    private final String TAG = MagTekCardReader.class.getSimpleName();

    private final Context context;
    private final String apiKey;
    // private final MagTekConnectionMethod connectionMethod;

    private final MTSCRA lib;

    private final MagTekBLEController bleController;
    private DeviceConnectionCallback deviceConnectionCallback;

    // state
    private boolean debug = false;

    // callback signatures

    public MagTekCardReader(Context context, String apiKey) {
        this.bleController = new MagTekBLEController(context);
        this.context = context;
        this.apiKey = apiKey;

        this.lib = new MTSCRA(context, new Handler(message -> {
            switch (message.what) {
                case OnDeviceConnectionStateChanged:
                    if (message.obj == MTConnectionState.Connected)
                        this.deviceConnectionCallback.callback(true);
                    break;
                default:
                    break;
            }
            return true;
        }));

        this.lib.setConnectionType(MTConnectionType.BLEEMV);
    }

    public void connect(String name, float timeout, DeviceConnectionCallback deviceConnection) {
        this.deviceConnectionCallback = deviceConnection;

        String address = this.bleController.getDeviceAddress(name);
        if (address == null)
            return;

        this.lib.setAddress(address);
        this.lib.openDevice();
    }

    public void disconnect() {
        this.lib.closeDevice();
    }

    public void startDeviceDiscovery(long timeout, DeviceDiscoveredCallback deviceDiscoveredCallback) {
        if (!this.bleController.isScanning()) {
            bleController.toggleScan(deviceDiscoveredCallback);
        }
    }

    public void stopDeviceDiscovery() {
        if (this.bleController.isScanning())
            bleController.toggleScan(null);
    }

    public void startTransaction(String amount, DeviceTransactionCallback deviceTransactionCallback) {
        // TODO
    }

    public void cancelTransaction() {
        this.lib.cancelTransaction();
    }

    public String getSerialNumber() {
        return this.lib.getDeviceSerial();
    }
}

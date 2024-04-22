package com.tripleplaypay.magteksdk;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRA;
import static com.magtek.mobile.android.mtlib.MTSCRAEvent.OnDeviceConnectionStateChanged;

public class MagTekCardReader {
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean debug = false;
    private String apiKey;
    private MTSCRA lib;
    private BluetoothAdapter bluetoothAdapter;

    private interface DeviceDiscovered { void callback(String name, int rssi); }
    private interface OnConnected { void callback(boolean connected); }

    private DeviceDiscovered deviceDiscovered;
    private OnConnected onConnected;

    private BluetoothAdapter setupBluetooth(Activity context) {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        // enable if not already in-use
        if (!adapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(context, enableBluetoothIntent, REQUEST_ENABLE_BT, new Bundle());
        }

        return adapter;
    }

    public MagTekCardReader(Activity context, String apiKey) {

        this.apiKey = apiKey;
        this.bluetoothAdapter = this.setupBluetooth(context);

        this.lib = new MTSCRA(context, new Handler(message -> {
            switch (message.what) {
                case OnDeviceConnectionStateChanged:
                    if (message.obj == MTConnectionState.Connected)
                        onConnected.callback(true);
                    break;
                default: break;
            }
            return true;
        }));

        this.lib.setConnectionType(MTConnectionType.BLEEMV);
    }

    public void startDeviceDiscovery(DeviceDiscovered deviceDiscovered) {
        this.deviceDiscovered = deviceDiscovered;
    }
}

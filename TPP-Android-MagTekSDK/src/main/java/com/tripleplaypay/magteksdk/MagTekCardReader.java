package com.tripleplaypay.magteksdk;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.magtek.mobile.android.mtlib.MTSCRA;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.sql.Array;
import java.util.HashSet;
import java.util.Set;


public class MagTekCardReader {
    // takes in the activity context
    private final Context context;

    private final String apiKey;
    private final MagTekConnectionMethod connectionMethod;

    private final MTSCRA lib;

    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private DeviceDiscovered deviceDiscovered;
    private DeviceConnection deviceConnection;

    private final BroadcastReceiver bluetoothDiscoveryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    Log.i("TAG", String.format("%s: %s", name, rssi));
            }
        }
    };

    // state
    private boolean debug = false;

    // callback signatures
    public interface DeviceDiscovered {
        void callback(String name, int rssi);
    }

    public interface DeviceConnection {
        void callback(boolean connected);
    }

    public interface DeviceTransaction {
        void callback(String message);
    }



    public MagTekCardReader(Context context, String apiKey, MagTekConnectionMethod connectionMethod) {
        this.context = context;
        this.apiKey = apiKey;
        this.connectionMethod = connectionMethod;

        this.lib = new MTSCRA(context, new Handler(message -> {
            switch (message.what) {
                default:
                    break;
            }
            return true;
        }));

        this.lib.setConnectionType(connectionMethod.type);
    }

    private void startScanForBluetoothDevices() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);

        this.context.registerReceiver(this.bluetoothDiscoveryReceiver, intentFilter);

        if (this.bluetoothAdapter.isEnabled()) {
            Log.i("TAG", "nice!");

            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            this.bluetoothAdapter.startDiscovery();
        }
    }

    /*
    Finds only connected MagTek devices
     MagTek Vendor ID: 0x801
     returns:
      ok - list of connected devices
      error - empty list
    */
    private UsbDevice[] getUsbDevices() {
        UsbManager manager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        if (manager == null)
            return new UsbDevice[]{};

        return (UsbDevice[]) manager.getDeviceList()
                .values()
                .stream()
                .filter(device -> device.getVendorId() == 0x801)
                .toArray(UsbDevice[]::new);
    }

    public void startDeviceDiscovery(DeviceDiscovered deviceDiscovered) {
        if (this.connectionMethod == MagTekConnectionMethod.Bluetooth) {
            this.deviceDiscovered = deviceDiscovered;
            this.startScanForBluetoothDevices();
        } else if (this.connectionMethod == MagTekConnectionMethod.USB) {
            for (UsbDevice device : this.getUsbDevices())
                deviceDiscovered.callback(device.getDeviceName(), 0);
        }
    }

    public void stopDeviceDiscovery() {
        if (this.connectionMethod == MagTekConnectionMethod.Bluetooth) {

            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            this.bluetoothAdapter.cancelDiscovery();
            this.context.unregisterReceiver(this.bluetoothDiscoveryReceiver);
        }
    }
}

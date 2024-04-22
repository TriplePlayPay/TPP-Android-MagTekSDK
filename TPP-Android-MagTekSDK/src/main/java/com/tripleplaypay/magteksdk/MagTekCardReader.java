package com.tripleplaypay.magteksdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import com.magtek.mobile.android.mtlib.MTSCRA;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;


public class MagTekCardReader {
    private final AppCompatActivity context;
    private final String apiKey;
    private final MagTekConnectionMethod connectionMethod;
    private final MTSCRA lib;
    private ActivityResultLauncher<Intent> intentLauncher;


    private boolean debug = false;

    interface DeviceDiscovered { void callback(String name, int rssi); }
    interface DeviceConnection { void callback(boolean connected); }
    interface DeviceTransaction { void callback(String message); }


    public MagTekCardReader(AppCompatActivity context, String apiKey, MagTekConnectionMethod connectionMethod) {
        this.context = context;
        this.apiKey = apiKey;
        this.connectionMethod = connectionMethod;

        this.lib = new MTSCRA(context, new Handler(message -> {
            switch (message.what) { default: break; }
            return true;
        }));

        this.lib.setConnectionType(connectionMethod.type);
    }


    private Set<BluetoothDevice> getBluetoothDevices() {
        BluetoothManager manager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        HashSet<BluetoothDevice> devices = new HashSet<>();

        if (!adapter.isEnabled()) { // enable bluetooth if it's turned off (or ask user to enable?)
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.intentLauncher.launch(enableBluetoothIntent);
        }

        // ... TODO: implement bluetooth scanning + discovery
        //           return only magtek devices

        return devices;
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
            return new UsbDevice[] {};

        return (UsbDevice[]) manager.getDeviceList()
                .values()
                .stream()
                .filter(device -> device.getVendorId() == 0x801)
                .toArray();
    }

    public void startDeviceDiscovery(DeviceDiscovered deviceDiscovered) {
        if (this.connectionMethod == MagTekConnectionMethod.Bluetooth) {
            // TODO: not implemented yet
        } else if (this.connectionMethod == MagTekConnectionMethod.USB) {
            for (UsbDevice device : getUsbDevices())
                deviceDiscovered.callback(device.getDeviceName(), 0);
        }
    }

    public void stopDeviceDiscovery() {
        if (this.connectionMethod == MagTekConnectionMethod.Bluetooth) {
            // TODO: cleanup bluetooth (?)
        }
    }
}

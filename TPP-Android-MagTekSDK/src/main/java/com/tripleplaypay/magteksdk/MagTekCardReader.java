package com.tripleplaypay.magteksdk;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTEMVEvent;
import com.magtek.mobile.android.mtlib.MTSCRAEvent;

public class MagTekCardReader {
    final String TAG = MagTekCardReader.class.getSimpleName();

    private final Activity activity;
    private final String apiKey;

    private final MTSCRA lib;

    private final MagTekBLESupport ble;

    private DeviceConnectionCallback deviceConnectionCallback;
    private DeviceTransactionCallback deviceTransactionCallback;

    private String lastTransactionMessage = "NO MESSAGE";
    private TransactionStatus lastTransactionStatus = TransactionStatus.noStatus;
    private TransactionEvent lastTransactionEvent = TransactionEvent.noEvents;

    private String deviceSerialNumber = "00000000000000000000000000000000";
    boolean deviceConnected = false;

    boolean debug = false;

    private String getTextFromBytes(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data)
            stringBuilder.append(String.format("%c", datum));
        return stringBuilder.toString();
    }

    public MagTekCardReader(Activity activity, String apiKey) {
        ble = new MagTekBLESupport(activity);
        this.activity = activity;
        this.apiKey = apiKey;

        lib = new MTSCRA(activity, new Handler(message -> {
            switch (message.what) {
                case MTSCRAEvent.OnDeviceConnectionStateChanged:
                    deviceConnected = message.obj == MTConnectionState.Connected;
                    if (deviceConnected)
                        emitDeviceConnected();
                    else if (message.obj == MTConnectionState.Disconnected || message.obj == MTConnectionState.Error)
                        emitDeviceDisconnected();
                    break;
                case MTEMVEvent.OnDisplayMessageRequest:
                    lastTransactionMessage = getTextFromBytes((byte[]) message.obj);
                    deviceTransactionCallback.callback(lastTransactionMessage, lastTransactionEvent, lastTransactionStatus);
                    break;
                case MTEMVEvent.OnTransactionStatus:
                    byte[] transactionStatusInfo = (byte[]) message.obj;
                    lastTransactionEvent = TransactionEvent.fromByte(transactionStatusInfo[0]);
                    lastTransactionStatus = TransactionStatus.fromByte(transactionStatusInfo[2]);
                    deviceTransactionCallback.callback(lastTransactionMessage, lastTransactionEvent, lastTransactionStatus);
                    break;
                case MTSCRAEvent.OnDeviceNotPaired:
                    deviceConnectionCallback.callback(false);
                    break;
                default:
                    break;
            }
            return true;
        }));

        lib.setConnectionType(MTConnectionType.BLEEMVT);
    }

    private void emitDeviceConnected() {
        lib.clearBuffers(); // reset device
        deviceSerialNumber = lib.getDeviceSerial();
        int msrResult = lib.sendCommandToDevice("580101"); // set MSR
        int bleResult = lib.sendCommandToDevice("480101"); // set BLE

        if (msrResult != 0)
            throw new RuntimeException("Could not set device to MSR mode");

        if (bleResult != 0)
            throw new RuntimeException("Could not put device into BLE mode");

        if (deviceConnectionCallback != null)
            deviceConnectionCallback.callback(true);
    }

    private void emitDeviceDisconnected() {
        if (deviceConnectionCallback != null)
            deviceConnectionCallback.callback(false);
    }

    public void connect(String name, DeviceConnectionCallback deviceConnectionCallback) {
        this.deviceConnectionCallback = deviceConnectionCallback;
        String address = ble.getDeviceAddress(name);
        if (address != null) {
            lib.setAddress(address);
            lib.openDevice();
        } else {
            Log.d(TAG, "connect: could not find a device with name" + name);
        }
    }

    public void disconnect() {
        lib.closeDevice();
    }

    public void startDeviceDiscovery(DeviceDiscoveredCallback deviceDiscoveredCallback) {
        ble.startScanningForPeripherals(deviceDiscoveredCallback);
    }

    public void stopDeviceDiscovery() {
        ble.stopScanningForPeripherals();
    }

    public void startTransaction(String amount, DeviceTransactionCallback deviceTransactionCallback) {
        if (debug) Log.d(TAG, "startTransaction: called with amount $" + amount);

        this.deviceTransactionCallback = deviceTransactionCallback;

        String n12format = String.format("%12.0f", Float.parseFloat(amount) * 100);
        byte[] amountBytes = new byte[6];

        int amountBytesIndex = 0;
        for (int i = 1; i < 12; i+=2) {
            String stringByte = n12format.substring(i-1, i).strip();
            if (stringByte.isEmpty())
                amountBytes[amountBytesIndex++] = 0;
            else
                amountBytes[amountBytesIndex++] = (byte) Integer.parseInt(stringByte, 16);
        }

        Log.d(TAG, "startTransaction: " + n12format);
        Log.d(TAG, "startTransaction: " + amountBytes[5]);


        byte[] cashBack = { 0, 0, 0, 0, 0, 0 };
        byte[] currency = { 0x08, 0x40 };

        this.lib.startTransaction(
                (byte) 0x3c,
                (byte) 7,
                (byte) 0,
                amountBytes,
                (byte) 0,
                cashBack,
                currency,
                (byte) 0x02
        );
    }

    public void cancelTransaction() {
        lib.cancelTransaction();
        if (debug) Log.d(TAG, "cancelTransaction: called");
    }

    public String getSerialNumber() {
        if (!deviceConnected)
            return "disconnected";
        return deviceSerialNumber;
    }
}

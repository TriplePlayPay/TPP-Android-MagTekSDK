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

import java.util.Timer;
import java.util.TimerTask;

public class MagTekCardReader {
    final String TAG = MagTekCardReader.class.getSimpleName();

    final Context context;
    final String apiKey;

    final MTSCRA lib;

    final MagTekBLEController bleController;

    DeviceConnectionCallback deviceConnectionCallback;
    DeviceTransactionCallback deviceTransactionCallback;

    String lastTransactionMessage;
    TransactionStatus lastTransactionStatus;
    TransactionEvent lastTransactionEvent;

    String deviceSerialNumber = "00000000000000000000000000000000";
    boolean deviceConnected = false;

    boolean debug = false;

    private String getTextFromBytes(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data)
            stringBuilder.append(String.format("%c", datum));
        return stringBuilder.toString();
    }

    public MagTekCardReader(Activity context, String apiKey) {
        bleController = new MagTekBLEController(context);
        this.context = context;
        this.apiKey = apiKey;

        lib = new MTSCRA(context, new Handler(message -> {
            switch (message.what) {
                case MTSCRAEvent.OnDeviceConnectionStateChanged:
                    deviceConnected = message.obj == MTConnectionState.Connected;
                    if (deviceConnected)
                        emitDeviceConnected();
                    else if (message.obj == MTConnectionState.Disconnected) // emit disconnected
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
        lib.clearBuffers();
        deviceSerialNumber = lib.getDeviceSerial();
        lib.sendCommandToDevice("580101"); // set MSR
        lib.sendCommandToDevice("480101"); // set BLE
        if (deviceConnectionCallback != null)
            deviceConnectionCallback.callback(true);
    }

    private void emitDeviceDisconnected() {
        if (deviceConnectionCallback != null)
            deviceConnectionCallback.callback(false);
    }

    public void connect(String name, float timeout, DeviceConnectionCallback deviceConnectionCallback) {
        this.deviceConnectionCallback = deviceConnectionCallback;

        String address = this.bleController.getDeviceAddress(name);
        if (address == null)
            return;

        lib.setAddress(address);
        lib.openDevice();
    }

    public void disconnect() {
        lib.closeDevice();
    }

    public void startDeviceDiscovery(long timeout, DeviceDiscoveredCallback deviceDiscoveredCallback) {
        if (!bleController.isScanning()) {
            bleController.toggleScan(deviceDiscoveredCallback);
        }
    }

    public void stopDeviceDiscovery() {
        if (bleController.isScanning())
            bleController.toggleScan(null);
    }

    public void startTransaction(String amount, DeviceTransactionCallback deviceTransactionCallback) {
        if (debug) Log.d(TAG, "startTransaction: called with amount $" + amount);

        this.deviceTransactionCallback = deviceTransactionCallback;

        String n12format = String.format("%12.0f", Float.parseFloat(amount) * 100);
        byte[] amountBytes = new byte[6];

        int amountBytesIndex = 0;
        for (int i = 1; i < 12; i+=2) {
            String stringByte = n12format.substring(i-1, i).strip();
            if (!stringByte.isEmpty()) {
                amountBytes[amountBytesIndex++] = (byte) Integer.parseInt(stringByte, 16);
            }
        }

        Log.d(TAG, "startTransaction: " + n12format);

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
        String serialNumber = lib.getDeviceSerial();
        if (debug) Log.d(TAG, "getSerialNumber: " + serialNumber);
        return serialNumber;
    }
}

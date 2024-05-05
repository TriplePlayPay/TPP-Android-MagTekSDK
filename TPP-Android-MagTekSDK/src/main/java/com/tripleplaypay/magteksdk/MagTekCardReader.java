package com.tripleplaypay.magteksdk;
import android.app.Activity;

public class MagTekCardReader {

    MagTekBLEController bleController;

    public MagTekCardReader(Activity activity, String apiKey, boolean debug, String debugUrl) {
        bleController = new MagTekBLEController(activity, apiKey, debug, debugUrl);
    }

    public MagTekCardReader(Activity activity, String apiKey, boolean debug) {
        this(activity, apiKey, debug, "https://www.tripleplaypay.com");
    }

    public MagTekCardReader(Activity activity, String apiKey) {
        this(activity, apiKey, false, "https://www.tripleplaypay.com");
    }

    public void startDeviceDiscovery(DeviceDiscoveredCallback deviceDiscoveredCallback) {
        bleController.deviceDiscoveredCallback = deviceDiscoveredCallback;
        bleController.startDeviceDiscovery();
    }

    public void cancelDeviceDiscovery() {
        bleController.deviceDiscoveredCallback = null;
        bleController.cancelDeviceDiscovery();
    }

    public void connect(String deviceName, int timeoutSeconds, DeviceConnectionCallback deviceConnectionCallback) {
        bleController.deviceConnectionCallback = deviceConnectionCallback;
        bleController.connect(deviceName, timeoutSeconds);
    }

    public void connect(String deviceName, DeviceConnectionCallback deviceConnectionCallback) {
        bleController.deviceConnectionCallback = deviceConnectionCallback;
        bleController.connect(deviceName, 10);
    }

    public void disconnect() {
        bleController.deviceConnectionCallback = null;
        bleController.disconnect();
    }

    public String getSerialNumber() {
        return bleController.getSerialNumber();
    }

    public void startTransaction(String amount, DeviceTransactionCallback deviceTransactionCallback) {
        bleController.deviceTransactionCallback = deviceTransactionCallback;
        bleController.startTransaction(amount);
    }

    public void cancelTransaction() {
        bleController.cancelTransaction();
        bleController.deviceTransactionCallback = null;
    }
}

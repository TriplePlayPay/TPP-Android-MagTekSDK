package com.tripleplaypay.magteksdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTEMVEvent;
import com.magtek.mobile.android.mtlib.MTSCRAEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MagTekBLEController {
    final String TAG = MagTekBLEController.class.getSimpleName();

    // Helper objects
    private final MagTekBLESupport ble;
    private final MTSCRA lib;

    // Constants
    private final String apiUrlEndpoint = "/api/emv";
    private final String apiUrl;
    private final String apiKey;
    private final boolean debug;

    // Callback refs
    public DeviceDiscoveredCallback deviceDiscoveredCallback;
    public DeviceConnectionCallback deviceConnectionCallback;
    public DeviceTransactionCallback deviceTransactionCallback;

    // device state
    private boolean lastApprovalValue = false;
    private String lastTransactionMessage = "NO MESSAGE";
    private TransactionStatus lastTransactionStatus = TransactionStatus.noStatus;
    private TransactionEvent lastTransactionEvent = TransactionEvent.noEvents;
    private String deviceSerialNumber = "00000000000000000000000000000000";
    private boolean deviceIsConnecting = false;
    private boolean deviceIsConnected = false;

    private String getTextFromBytes(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            if (datum == 0)
                break;
            stringBuilder.append(String.format("%c", datum));
        }
        return stringBuilder.toString();
    }

    @SuppressLint("DefaultLocale")
    private byte[] getBytes(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];
        int bytesIndex = 0;
        for (int i = 1; i < hexString.length(); i+=2) {
            byte parsedByte = (byte) (Integer.parseInt(hexString.substring(i - 1, i + 1), 16) & 0xff);
            bytes[bytesIndex++] = parsedByte;
        }
        return bytes;
    }

    private String getHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte stringByte : bytes)
            stringBuilder.append(String.format("%02X", stringByte));
        return stringBuilder.toString();
    }

    public MagTekBLEController(Activity activity, String apiKey, boolean debug, String debugUrl) {
        ble = new MagTekBLESupport(activity);
        this.apiUrl = debugUrl;
        this.apiKey = apiKey;
        this.debug = debug;

        lib = new MTSCRA(activity, new Handler(message -> {
            switch (message.what) {
                case MTSCRAEvent.OnDeviceConnectionStateChanged:
                    deviceConnectionStateChanged((MTConnectionState) message.obj);
                    break;
                case MTEMVEvent.OnDisplayMessageRequest:
                    displayMessageRequest((byte[]) message.obj);
                    break;
                case MTEMVEvent.OnTransactionStatus:
                    transactionStatus((byte[]) message.obj);
                    break;
                case MTEMVEvent.OnARQCReceived:
                    sendARQCRequest((byte[]) message.obj);
                    break;
            }
            return true;
        }));

        lib.setConnectionType(MTConnectionType.BLEEMVT);
    }

    private void deviceConnectionStateChanged(MTConnectionState connectionState) {
        if (deviceIsConnecting) {
            deviceIsConnected = connectionState == MTConnectionState.Connected;
            if (deviceIsConnected)
                emitDeviceConnected();
            else if (connectionState == MTConnectionState.Disconnected || connectionState == MTConnectionState.Error)
                emitDeviceDisconnected();
        }
    }

    private void displayMessageRequest(byte[] message) {
        lastTransactionMessage = getTextFromBytes(message);
        if (lastTransactionMessage.equals("TRANSACTION TERMINATED")) { // this means ARQC was passed in
            if (lastApprovalValue) {
                lastApprovalValue = false; // if this gets called it means the user was notified, reset the value
                lastTransactionMessage = "APPROVED";
            } else
                lastTransactionMessage = "DECLINED";
        }
        deviceTransactionCallback.onDeviceTransactionInfo(lastTransactionMessage, lastTransactionEvent, lastTransactionStatus);
    }

    private void transactionStatus(byte[] transactionStatusInfo) {
        lastTransactionEvent = TransactionEvent.fromByte(transactionStatusInfo[0]);
        lastTransactionStatus = TransactionStatus.fromByte(transactionStatusInfo[2]);
        deviceTransactionCallback.onDeviceTransactionInfo(lastTransactionMessage, lastTransactionEvent, lastTransactionStatus);
    }

    private HttpURLConnection openHttpConnection(String url) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Content-Type", "application/json");
        httpConnection.setRequestProperty("Authorization", apiKey);
        httpConnection.setDoOutput(true);
        httpConnection.setChunkedStreamingMode(0);
        return httpConnection;
    }

    private JSONObject loadJSONPayload(String arqcHexString) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("payload", arqcHexString);
        return jsonObject;
    }

    private JSONObject sendHttpRequest(HttpURLConnection httpConnection, String arqcHexString) throws IOException, JSONException {
        JSONObject requestObject = loadJSONPayload(arqcHexString);
        byte[] responseBuffer = new byte[4096]; // give 4KiB max

        OutputStream outputStream = new BufferedOutputStream(httpConnection.getOutputStream());
        outputStream.write(requestObject.toString().getBytes());
        outputStream.close();

        InputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
        if (inputStream.read(responseBuffer) < 0)
            throw new IOException("Could not read from inputStream");

        return new JSONObject(getTextFromBytes(responseBuffer));
    }

    private void sendARQCRequest(byte[] arqc) {
        if (debug) Log.d(TAG, "sendARQCRequest: URL => " + apiUrl);

        // set strict mode (idk why, but this is required for network calls)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String arqcHexString = getHexString(arqc);

        if (debug) Log.d(TAG, "sendARQCRequest: ARQC => " + arqcHexString);

        try {
            HttpURLConnection httpConnection = openHttpConnection(apiUrl + apiUrlEndpoint);
            JSONObject responseObject = sendHttpRequest(httpConnection, arqcHexString);

            if (responseObject.getBoolean("status")) {
                JSONObject message = responseObject.getJSONObject("message");

                byte[] arpc = getBytes(message.getString("arpc"));
                lastApprovalValue = message.getBoolean("approved");

                if (debug) Log.i(TAG, "sendARQCRequest: " + getHexString(arpc));

                lib.setAcquirerResponse(arpc);
            } else {
                Log.e(TAG, "sendARQCRequest: API error => " + responseObject.getString("error"));
                lib.cancelTransaction();
            }
        } catch (IOException exception) {
            Log.e(TAG, "sendARQCRequest: IO error => " + exception.getMessage());
        } catch (JSONException exception) {
            Log.e(TAG, "sendARQCRequest: JSON error => " + exception.getMessage());
        }
    }

    private void emitDeviceConnected() {
        lib.clearBuffers(); // reset device
        deviceSerialNumber = lib.getDeviceSerial();
        lib.sendCommandToDevice("580101"); // set MSR
        lib.sendCommandToDevice("480101"); // set BLE
        if (deviceConnectionCallback != null)
            deviceConnectionCallback.onDeviceConnection(true);
        deviceIsConnecting = false;
    }

    private void emitDeviceDisconnected() {
        if (deviceConnectionCallback != null)
            deviceConnectionCallback.onDeviceConnection(false);
    }

    public void connect(String name, int timeout) {
        String address = ble.getDeviceAddress(name);

        if (address != null) {
            deviceIsConnecting = true;
            lib.setAddress(address);
            lib.openDevice();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                CompletableFuture.delayedExecutor(timeout, TimeUnit.SECONDS).execute(() -> {
                    if (!deviceIsConnected) { // trigger the callback if timeout triggers
                        deviceConnectionCallback.onDeviceConnection(false);
                        lib.closeDevice(); // make sure to cancel connection attempt
                    }
                    deviceIsConnecting = false;
                });
            } else {
                throw new RuntimeException("SDK version not compatible");
            }
        } else {
            Log.d(TAG, "connect: could not find a device with name '" + name + "'");
        }
    }

    public void disconnect() {
        if (debug) Log.d(TAG, "disconnect: called");
        lib.closeDevice();
    }

    public void startDeviceDiscovery() {
        if (debug) Log.d(TAG, "startDeviceDiscovery: called");
        ble.startScanningForPeripherals(deviceDiscoveredCallback);
    }

    public void cancelDeviceDiscovery() {
        if (debug) Log.d(TAG, "stopDeviceDiscovery: called");
        ble.stopScanningForPeripherals();
    }

    @SuppressLint("DefaultLocale")
    public void startTransaction(String amount) {
        if (debug) Log.d(TAG, "startTransaction: called with amount $" + amount);

        byte[] amountBytes = new byte[6];
        String n12format = String.format("%012.0f", Float.parseFloat(amount) * 100);

        if (debug) Log.d(TAG, "startTransaction: " + n12format);

        int amountBytesIndex = 0;
        for (int i = 1; i < 12; i+=2) {
            String stringByte = n12format.substring(i - 1, i + 1).strip();
            if (stringByte.isEmpty())
                amountBytes[amountBytesIndex++] = 0;
            else
                amountBytes[amountBytesIndex++] = (byte) (Integer.parseInt(stringByte, 16) & 0xff);
        }

        lib.startTransaction(
                (byte) 0x3c,
                (byte) 7,
                (byte) 0,
                amountBytes,
                (byte) 0,
                new byte[] { 0, 0, 0, 0, 0, 0 },
                new byte[] { 0x08, 0x40 },
                (byte) 0x02
        );
    }

    public void cancelTransaction() {
        if (debug) Log.d(TAG, "cancelTransaction: called");
        lib.cancelTransaction();
    }

    public String getSerialNumber() {
        if (debug) Log.d(TAG, "getSerialNumber: called");
        if (!deviceIsConnected)
            return "disconnected";
        return deviceSerialNumber;
    }
}

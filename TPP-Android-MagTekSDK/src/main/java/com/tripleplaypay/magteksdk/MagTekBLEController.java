package com.tripleplaypay.magteksdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import com.magtek.mobile.android.mtlib.MTBaseService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MagTekBLEController {
    final String publicMethodTag = MagTekBLEController.class.getSimpleName();
    final String mtscraMethodTag = "MTSCRA";

    // Helper objects
    private final MagTekBLESupport ble;
    private final MTSCRA lib;

    // Constants
    private final String apiUrlEndpoint = "/api/emv";
    private boolean debug = false;

    private final String apiUrl;
    private final String apiKey;

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

    /* Initialize:
     * activity: Activity
     *  the activity from the parent UI object. needed for bluetooth permission dialogs + MTSCRA
     * apiKey: String
     *  needs an API key to communicate with Triple Play Pay
     * apiUrl: String
     *  needs to know which Triple Play Pay API to query
     */

    public MagTekBLEController(Activity activity, String apiKey, String debugUrl) {
        this.apiUrl = debugUrl;
        this.apiKey = apiKey;

        ble = new MagTekBLESupport(activity); // BLE helper needed for Android
        lib = new MTSCRA(activity, new Handler(message -> {
            switch (message.what) {
                case MTSCRAEvent.OnDeviceConnectionStateChanged:
                    onDeviceConnectionStateChanged((MTConnectionState) message.obj);
                    break;
                case MTEMVEvent.OnDisplayMessageRequest:
                    onDisplayMessageRequest((byte[]) message.obj);
                    break;
                case MTEMVEvent.OnTransactionStatus:
                    onTransactionStatus((byte[]) message.obj);
                    break;
                case MTEMVEvent.OnARQCReceived:
                    onARQCReceived((byte[]) message.obj);
                    break;
            }
            return true;
        }));

        // NOTE: setDeviceType is missing from the Android MTSCRA
        lib.setConnectionType(MTConnectionType.BLEEMVT);
    }

    /* Private functions for simple processes
     * - debugPrint (tag: String, message: String) -> prints a debug message to STDOUT. Takes a tag argument for better organization
     * - emitDeviceIsConnected () -> should get called when the device is determined to be connected; Configures the device
     * - emitDeviceDisconnected () -> should get called when the device has been disconnected
     * - hexStringBytes (string: String): byte[] -> needed to parse response from Triple Play Pay API and pass to aquirerResponse. also useful for parsing data from device
     * - dataToHexString (data: byte[]): String -> inversion of the above method
     * - getTextFromBytes (data: byte[]): String -> converts a byte array into a UTF-8 string
     * - n12Bytes (amount: String): byte[] -> converts dollar amount string into n12 byte array
     * - openHttpConnection (url: String) -> opens an HTTP(S) connection to the url, sets POST + JSON options + API key
     * - loadJSONPayload (arqcHexString: String) -> creates a JSON object to send over the HTTP(S) connection
     * - sendHttpRequest (request: HttpUrlConnection, String arqcHexString) -> sends the ARQC in a JSON payload using the created request
     */

    private void debugPrint(String tag, String message) {
        if (debug) Log.d(tag, message);
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
        deviceIsConnecting = false;
    }

    @SuppressLint("DefaultLocale")
    private byte[] hexStringBytes(String hexString) {
        int length = hexString.length();
        byte[] bytes = new byte[length / 2];
        int bytesIndex = 0;
        for (int i = 1; i < length; i+=2) {
            byte parsedByte = (byte) (Integer.parseInt(hexString.substring(i - 1, i + 1), 16) & 0xff);
            bytes[bytesIndex++] = parsedByte;
        }
        return bytes;
    }

    private String dataToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte stringByte : bytes)
            stringBuilder.append(String.format("%02X", stringByte));
        return stringBuilder.toString();
    }

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
    private byte[] n12Bytes(String amount) {
        String formattedString = String.format("%012.0f", Double.parseDouble(amount) * 100.0);
        return hexStringBytes(formattedString);
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

    private JSONObject sendHttpRequest(HttpURLConnection request, String arqcHexString) throws IOException, JSONException {
        JSONObject requestObject = loadJSONPayload(arqcHexString);
        byte[] responseBuffer = new byte[4096]; // give 4KiB max

        OutputStream outputStream = new BufferedOutputStream(request.getOutputStream());
        outputStream.write(requestObject.toString().getBytes());
        outputStream.close();

        InputStream inputStream = new BufferedInputStream(request.getInputStream());
        if (inputStream.read(responseBuffer) < 0)
            throw new IOException("Could not read from inputStream");

        return new JSONObject(getTextFromBytes(responseBuffer));
    }

    /* Callback functions for MTSCRA. These are called when an event happens ON THE DEVICE
     *  - onDeviceConnectionStateChanged(state: MTConnectionState) -> `state` can be Connected, Connecting, Disconnected, Disconnecting, Error.
     *  - onTransactionStatus (data: byte[]) -> `data` is a byte buffer containing the current transaction status and event
     *  - onDisplayRequestMethod (data: byte[]) -> `data` is a byte buffer that can be translated to a UTF-8 string. This is a message from the device to the cardholder
     *  - onARQCReceived (data: byte[]) -> `data` is a byte buffer from the device of the encrypted ARQC data to be processed by the Triple Play Pay API
     */

    private void onDeviceConnectionStateChanged(MTConnectionState state) {
        debugPrint(mtscraMethodTag, "deviceConnectionStateChanged called");
        if (deviceIsConnecting) {
            deviceIsConnected = state == MTConnectionState.Connected;
            if (deviceIsConnected)
                emitDeviceConnected();
            else if (state == MTConnectionState.Disconnected || state == MTConnectionState.Error)
                emitDeviceDisconnected();
        }
    }

    private void onTransactionStatus(byte[] data) {
        lastTransactionEvent = TransactionEvent.fromByte(data[0]);
        lastTransactionStatus = TransactionStatus.fromByte(data[2]);
        debugPrint(mtscraMethodTag, "transactionStatus called");
        if (deviceTransactionCallback != null)
            deviceTransactionCallback.onDeviceTransactionInfo(lastTransactionMessage, lastTransactionEvent, lastTransactionStatus);
    }

    private void onDisplayMessageRequest(byte[] data) {
        lastTransactionMessage = getTextFromBytes(data);
        if (lastTransactionMessage.equals("TRANSACTION TERMINATED")) { // this means ARQC was passed in
            if (lastApprovalValue) {
                lastApprovalValue = false; // if this gets called it means the user was notified, reset the value
                lastTransactionMessage = "APPROVED";
            } else
                lastTransactionMessage = "DECLINED";
        }
        debugPrint(mtscraMethodTag, "displayMessageRequest called");
        if (deviceTransactionCallback != null)
            deviceTransactionCallback.onDeviceTransactionInfo(lastTransactionMessage, lastTransactionEvent, lastTransactionStatus);
    }

    private void onARQCReceived(byte[] data) {
        debugPrint(mtscraMethodTag, "sendARQCRequest: URL => " + apiUrl);

        // set strict mode (idk why, but this is required for network calls)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String arqcHexString = dataToHexString(data);

        debugPrint(mtscraMethodTag, "sendARQCRequest: ARQC => " + arqcHexString);
        try {
            HttpURLConnection request = openHttpConnection(apiUrl + apiUrlEndpoint);
            JSONObject responseObject = sendHttpRequest(request, arqcHexString);

            if (responseObject.getBoolean("status")) {
                JSONObject message = responseObject.getJSONObject("message");

                byte[] arpcData = hexStringBytes(message.getString("arpc"));
                lastApprovalValue = message.getBoolean("approved");

                debugPrint(mtscraMethodTag, "sendARQCRequest: " + dataToHexString(arpcData));

                lib.setAcquirerResponse(arpcData);
            } else {
                Log.e(mtscraMethodTag, "sendARQCRequest: API error => " + responseObject.getString("error"));
                lib.cancelTransaction();
            }
        } catch (IOException exception) {
            lib.cancelTransaction();
            Log.e(mtscraMethodTag, "sendARQCRequest: IO error => " + exception.getMessage());
        } catch (JSONException exception) {
            lib.cancelTransaction();
            Log.e(mtscraMethodTag, "sendARQCRequest: JSON error => " + exception.getMessage());
        }
    }

    /* Public functions for MagTekCardReader
     * General:
     *  - isConnected (): boolean -> gives the current connection state
     *  - getSerialNumber (): String -> gets the serial number of the device
     *  - setDebug (debug: boolean) -> sets the MTSCRA and TPP debug print statements to go to stdout
     * Discovery:
     *  - startDeviceDiscovery () -> tells the phone to begin a bluetooth LE device scan
     *  - cancelDeviceDiscovery () -> tells the phone to stop scanning for LE devices
     * Connection:
     *  - connect (name: String, timeout: int) -> tells the phone to connect to the device with `name` and
                                                            cancel itself after `timeout` seconds have passed
     *  - disconnect () -> tells the phone to stop attempting to connect
     * Transactions:
     *  - startTransaction (amount: String) -> begins a transaction process on the device with `amount` being charged to the card
     *  - cancelTransaction () -> cancels a running transaction
     */

    public boolean isConnected() {
        return lib.isDeviceConnected() && deviceIsConnected;
    }

    public String getSerialNumber() {
        debugPrint(publicMethodTag, "getSerialNumber: " + deviceSerialNumber);
        if (!deviceIsConnected)
            return "disconnected";
        return deviceSerialNumber;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void startDeviceDiscovery() {
        debugPrint(publicMethodTag, "startDeviceDiscovery: called");
        ble.startScanningForPeripherals(deviceDiscoveredCallback);
    }

    public void cancelDeviceDiscovery() {
        debugPrint(publicMethodTag, "stopDeviceDiscovery: called");
        ble.stopScanningForPeripherals();
    }

    public void connect(String name, int timeout) {
        String address = ble.getDeviceAddress(name);
        if (address != null) {
            deviceIsConnecting = true;
            lib.setAddress(address);
            lib.openDevice();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                CompletableFuture.delayedExecutor(timeout, TimeUnit.SECONDS).execute(() -> {
                    if (deviceIsConnecting) {
                        debugPrint("CONNECT TIMEOUT", String.format("called after %d seconds", timeout));
                        if (deviceIsConnected) {
                            emitDeviceConnected();
                        } else {
                            emitDeviceDisconnected();
                        }
                        deviceIsConnecting = false;
                    }
                });
            } else {
                throw new RuntimeException("SDK version not compatible");
            }
        } else {
            Log.e(publicMethodTag, "connect: could not find a device with name '" + name + "'");
        }
    }

    public void disconnect() {
        debugPrint(publicMethodTag, "disconnect: called");
        lib.closeDevice();
    }

    @SuppressLint("DefaultLocale")
    public void startTransaction(String amount) {
        debugPrint(publicMethodTag, "startTransaction: called with amount $" + amount);
        lib.startTransaction(
                (byte) 0x3c,
                (byte) 7,
                (byte) 0,
                n12Bytes(amount),
                (byte) 0,
                new byte[] { 0, 0, 0, 0, 0, 0 },
                new byte[] { 0x08, 0x40 },
                (byte) 0x02
        );
    }

    public void cancelTransaction() {
        debugPrint(publicMethodTag, "cancelTransaction: called");
        lib.cancelTransaction();
    }


}

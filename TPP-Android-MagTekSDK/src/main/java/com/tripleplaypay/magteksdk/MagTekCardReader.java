package com.tripleplaypay.magteksdk;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTEMVEvent;

import static com.magtek.mobile.android.mtlib.MTSCRAEvent.OnDeviceConnectionStateChanged;

public class MagTekCardReader {
    private final String TAG = MagTekCardReader.class.getSimpleName();

    private final Context context;
    private final String apiKey;
    // private final MagTekConnectionMethod connectionMethod;

    private final MTSCRA lib;

    private final MagTekBLEController bleController;
    private DeviceConnectionCallback deviceConnectionCallback;
    private DeviceTransactionCallback deviceTransactionCallback;

    // state
    private boolean debug = false;

    // callback signatures

    private String getTextString(byte[] data, int start, int length)
    {
        String result = "";

        if (data != null && data.length > 0)
        {
            StringBuilder stringBuilder = new StringBuilder(data.length+1);
            for(int i = start; i < length; i++)
            {
                try
                {
                    stringBuilder.append(String.format("%c", data[i]));
                }
                catch (Exception ex)
                {
                    stringBuilder.append("<?>");
                }
            }
            result = stringBuilder.toString();
        }

        return result;
    }

    public MagTekCardReader(Activity context, String apiKey) {
        this.bleController = new MagTekBLEController(context);
        this.context = context;
        this.apiKey = apiKey;

        this.lib = new MTSCRA(context, new Handler(message -> {
            switch (message.what) {
                case OnDeviceConnectionStateChanged:
                    if (message.obj == MTConnectionState.Connected)
                        this.deviceConnectionCallback.callback(true);
                    break;
                case MTEMVEvent.OnDisplayMessageRequest:
                    byte[] bytes = (byte[]) message.obj;
                    this.deviceTransactionCallback.callback(getTextString(bytes, 0, bytes.length), null, null);
                default:
                    break;
            }
            return true;
        }));

        this.lib.setConnectionType(MTConnectionType.BLEEMVT);
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
        this.lib.cancelTransaction();
    }

    public String getSerialNumber() {
        return this.lib.getDeviceSerial();
    }
}

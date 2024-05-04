package com.tripleplaypay.magtekdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.tripleplaypay.magteksdk.MagTekCardReader;

import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AtomicReference<String> deviceName = new AtomicReference<>("no name");
        MagTekCardReader cardReader = new MagTekCardReader(this, "test-api-key");

        TextView debugText = findViewById(R.id.debug_text);

        Button startTransactionButton = findViewById(R.id.start_transaction_button);
        startTransactionButton.setOnClickListener(view -> {
            cardReader.startTransaction("1.23", (message, event, status) -> {
                Log.d(TAG, "startTransaction: " + event.toString() + ", " + status.toString());
                debugText.setText(message);
            });
        });

        Button stopTransactionButton = findViewById(R.id.stop_transaction_button);
        stopTransactionButton.setOnClickListener(view -> {
            cardReader.cancelTransaction();
        });

        Button testButton = findViewById(R.id.test_button);
        testButton.setOnClickListener(view -> {
            cardReader.startDeviceDiscovery((name, rssi) -> {
                deviceName.set(name);
                debugText.setText(name);
            });
        });

        Button disconnectButton = findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(view -> {
            cardReader.disconnect();
        });

        Button connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(view -> {
            debugText.setText("connecting...");
            cardReader.connect(deviceName.get(), connected -> {
                if (connected) {
                    debugText.setText(cardReader.getSerialNumber());
                } else {
                    debugText.setText("disconnected");
                }
            });
        });

        Button testCancelButton = findViewById(R.id.test_cancel_button);
        testCancelButton.setOnClickListener(view -> {
            cardReader.stopDeviceDiscovery();
        });
    }
}
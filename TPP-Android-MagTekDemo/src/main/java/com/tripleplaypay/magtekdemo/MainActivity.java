package com.tripleplaypay.magtekdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.tripleplaypay.magteksdk.MagTekCardReader;

import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AtomicReference<String> deviceName = new AtomicReference<>("no name");
        MagTekCardReader cardReader = new MagTekCardReader(this, "test-api-key");

        TextView debugText = findViewById(R.id.debug_text);

        Button testButton = findViewById(R.id.test_button);
        testButton.setOnClickListener(view -> {
            cardReader.startDeviceDiscovery(10000, (name, rssi) -> {
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
            cardReader.connect(deviceName.get(), 10000, connected -> {
                connectButton.setText(connected ? "connected" : "disconnected");
            });
        });

        Button testCancelButton = findViewById(R.id.test_cancel_button);
        testCancelButton.setOnClickListener(view -> {
            cardReader.stopDeviceDiscovery();
        });
    }
}
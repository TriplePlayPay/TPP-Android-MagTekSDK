package com.tripleplaypay.magtekdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.tripleplaypay.magteksdk.MagTekCardReader;
import com.tripleplaypay.magteksdk.MagTekConnectionMethod;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private MagTekCardReader cardReader;
    private final String apiKey = "test-api-key";
    private String usbAddress = "";

    public TextView debugText;
    public Button testButton;
    public Button connectButton;
    public Button disconnectButton;
    public Button testCancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        debugText = findViewById(R.id.debug_text);
        testButton = findViewById(R.id.test_button);
        connectButton = findViewById(R.id.connect_button);
        disconnectButton = findViewById(R.id.disconnect_button);
        testCancelButton = findViewById(R.id.test_cancel_button);

        cardReader = new MagTekCardReader(this, apiKey, MagTekConnectionMethod.USB);

        testButton.setOnClickListener(view -> {
            cardReader.startDeviceDiscovery((name, rssi) -> {
                debugText.setText(name);
                Log.i("TAG", name);
            });
        });

        disconnectButton.setOnClickListener(view -> {
            cardReader.disconnect();
        });

        connectButton.setOnClickListener(view -> {
            cardReader.connect(usbAddress, connected -> {
                connectButton.setText(connected ? "connected" : "disconnected");
            });
        });

        testCancelButton.setOnClickListener(view -> {
            cardReader.stopDeviceDiscovery();
        });
    }
}
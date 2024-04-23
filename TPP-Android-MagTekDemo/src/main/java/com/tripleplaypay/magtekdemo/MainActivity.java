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

    public TextView debugText;
    public Button testButton;
    public Button testCancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        debugText = findViewById(R.id.debug_text);
        testButton = findViewById(R.id.test_button);
        testCancelButton = findViewById(R.id.test_cancel_button);

        cardReader = new MagTekCardReader(this, apiKey, MagTekConnectionMethod.Bluetooth);

        testButton.setOnClickListener(view -> {
            cardReader.startDeviceDiscovery((name, rssi) -> {
                debugText.setText(name);
                Log.i("TAG", name);
            });
        });

        testCancelButton.setOnClickListener(view -> {
            cardReader.stopDeviceDiscovery();
        });
    }
}
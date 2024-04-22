package com.tripleplaypay.magteksdk;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class TestClass extends AppCompatActivity {

    private final String apiKey = "1234123412341234";

    MagTekCardReader cardReader;

    TestClass() {
        cardReader = new MagTekCardReader(this, apiKey, MagTekConnectionMethod.USB);

        cardReader.startDeviceDiscovery((name, rssi) -> {
            Log.i("TEST", name);
        });
    }

}

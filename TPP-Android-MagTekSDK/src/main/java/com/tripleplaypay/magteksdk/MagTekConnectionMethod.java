package com.tripleplaypay.magteksdk;

import com.magtek.mobile.android.mtlib.MTConnectionType;

public enum MagTekConnectionMethod {
    USB(MTConnectionType.USB), Bluetooth(MTConnectionType.BLEEMV);

    public final MTConnectionType type;
    MagTekConnectionMethod(MTConnectionType type) {
        this.type = type;
    }
}

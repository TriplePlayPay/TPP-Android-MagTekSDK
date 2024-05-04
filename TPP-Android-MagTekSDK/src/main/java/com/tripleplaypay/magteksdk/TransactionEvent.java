package com.tripleplaypay.magteksdk;

import android.util.Log;

public enum TransactionEvent {

    noEvents(0),
    cardInserted(1),
    paymentMethodError(2),
    progressChange(3),
    waiting(4),
    timeout(5),
    complete(6),
    canceled(7),
    cardRemoved(8),
    contactless(9),
    cardSwipe(10),
    ;
    final static String TAG = TransactionEvent.class.getSimpleName();
    final int code;

    static public TransactionEvent fromByte(byte code) {
        for (TransactionEvent number : TransactionEvent.values()) {
            if ((byte) number.code == code)
                return number;
        }
        throw new RuntimeException("Could not find enum with value " + code);
    }

    TransactionEvent(int code) {
        this.code = code;
    }
}

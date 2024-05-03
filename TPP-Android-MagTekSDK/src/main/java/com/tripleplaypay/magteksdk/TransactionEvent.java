package com.tripleplaypay.magteksdk;

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
    final int code;

    public static TransactionEvent fromByte(byte code) {
        return TransactionEvent.values()[code];
    }

    TransactionEvent(int code) {
        this.code = code;
    }
}

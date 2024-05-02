package com.tripleplaypay.magteksdk;

public interface DeviceTransactionCallback {
    void callback(String name, TransactionEvent event, TransactionStatus status);
}

package com.tripleplaypay.magteksdk;

public interface DeviceTransactionCallback {
    void onDeviceTransactionInfo(String name, TransactionEvent event, TransactionStatus status);
}

package com.tripleplaypay.magteksdk;

public enum TransactionStatus {
    noStatus(0),
    waiting(1),
    reading(2),
    selectingApplication(3),
    selectingCardholderLanguage(4),
    selectingCardholderApplication(5),
    initiatingApplication(6),
    readingApplicationData(7),
    offlineAuthentication(8),
    processingRestrictions(9),
    cardholderVerification(10),
    terminalRiskManagement(11),
    terminalActionAnalysis(12),
    generatingCryptogram(13),
    cardActionAnalysis(14),
    onlineProcessing(15),
    waitingForProcessing(16),
    complete(17),
    error(18),
    approved(19),
    declined(20),
    canceledByMSR(21),
    emvConditionsNotSatisfied(22),
    emvCardBlocked(23),
    contactSelectionFailed(24),
    emvCardNotAccepted(25),
    emptyCandidateList(26),
    applicationBlocked(27),
    hostCanceled(145),
    applicationSelectionFailed(40),
    removedCard(41),
    collisionDetected(42),
    referToHandheldDevice(43),
    contactlessComplete(44),
    requestSwitchToMSR(45),
    wrongCardType(46),
    noInterchangeProfile(47),
    ;
    final int code;

    static public TransactionStatus fromByte(byte code) {
        for (TransactionStatus number : TransactionStatus.values()) {
            if (number.code == code)
                return number;
        }
        // the device can throw random shit so just silently fail with noStatus when that happens
        return TransactionStatus.noStatus;
    }

    TransactionStatus(int code) { this.code = code; }
}

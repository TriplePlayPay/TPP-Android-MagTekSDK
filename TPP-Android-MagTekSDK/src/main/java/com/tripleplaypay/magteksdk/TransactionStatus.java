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

    public static TransactionStatus fromByte(byte code) {
        return TransactionStatus.values()[code];
    }

    TransactionStatus(int code) {
        this.code = code;
    }
}

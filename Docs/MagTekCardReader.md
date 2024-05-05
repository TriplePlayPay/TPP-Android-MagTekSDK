# MagTekCardReader
This is the main class for the SDK. it provides all of the functionality needed to scan for, connect to, and make transactions with MagTek tDyanmo devices.
## Init
In order to initialize the card reader interface, you must pass in your _Triple Play Pay_ API key.
```java
MagTekCardReader cardReader = new MagTekCardReader("b5918e9a-c50f-491a-bcae-d52e0556c498");
```
## Start Device Discovery
Before you can connect to a device, you need to scan for available targets. In order to do this, you can simply call `startDeviceDiscovery` on a `MagTekCardReader` object. The function accepts one argument which is a callback. The callback accepts two arguments: `(String, int)`. These two values represent the name of the device and the signal strength
```java
// this function only accepts a callback with two arguments
cardReader.startDeviceDiscovery((name, rssi) -> {
    // add the newly discovered device to some sort of container to display in the UI
    deviceTable.put(name, rssi); // assuming a 'Hashtable<String, int> deviceTable;' is being used
});
```
## Cancel Device Discovery
Once you've found the device to pair with, it's a good idea to stop the internal radio from scanning. Use the `cancelDeviceDiscovery` method to cancel the scan.
```java
cardReader.cancelDeviceDiscovery();
```
## Connect
In order to connect to a device, you need its name from the `startDeviceDiscovery` method. The `connect` method takes in three arguments: the first one is the `String` representing the device's name. The second argument is an optional `int` timeout value in seconds. The default timeout is `10` seconds. After the timeout completes, the device will stop attempting to connect. The third argument is a callback. This callback accepts one argument which is a `boolean` that represents if the device has been connected or not. if the device has not connected after the timeout, the callback will be returned with `false` as the argument.
```java
cardReader.connect(deviceName, 5, connected -> {
    if (connected) {
        // update UI to say connected
    } else {
        // update UI to tell user connection failed
    }
});
```
## Disconnect
In order to sever the connection, use the `disconnect` method
```java
cardReader.disconnect();
```
## Start Transaction
Once the device is connected, you will have the ability to initiate an EMV transaction. The method `startTransaction` takes two arguments. The first argument is a `String` representing the dollar amount of the transaction. The decimal `.` must be in the string. The second argument is a callback which takes in three arguments: a `String` which represents a message the device is trying to communicate to the end user, a `TransactionEvent` which updates each time the device has an event to report, and a `TransactionStatus` which updates each time the device's status changes throughout a transaction.
```java
cardReader.startTransaction("1.01", (message, event, status) -> {
    // check to see if the transaction is completed
    if (event == TransactionEvent.complete) {
        // reset UI
    }

    if (status == TransactionStatus.onlineProcessing) {
        // show loading icon
    }
    
    // set some UI component to the current message
    transactionMessage = message;
})
```
## Cancel Transaction
In order to prematurely cancel a transaction, you can use the `cancelTransaction` method
```java
cardReader.cancelTransaction();
```
### Enums
#### Transaction Events
- noEvents: 0
- cardInserted: 1
- paymentMethodError: 2
- progressChange: 3
- waiting: 4
- timeout: 5
- complete: 6
- canceled: 7
- cardRemoved: 8
- contactless: 9
- cardSwipe: 10
#### Transaction Statuses
- noStatus: 0
- waiting: 1
- reading: 2
- selectingApplication: 3
- selectingCardholderLanguage: 4
- selectingCardholderApplication: 5
- initiatingApplication: 6
- readingApplicationData: 7
- offlineAuthentication: 8
- processingRestrictions: 9
- cardholderVerification: 10
- terminalRiskManagement: 11
- terminalActionAnalysis: 12
- generatingCryptogram: 13
- cardActionAnalysis: 14
- onlineProcessing: 15
- waitingForProcessing: 16
- complete: 17
- error: 18
- approved: 19
- declined: 20
- canceledByMSR: 21
- emvConditionsNotSatisfied: 22,
- emvCardBlocked: 23,
- contactSelectionFailed: 24,
- emvCardNotAccepted: 25,
- emptyCandidateList: 26,
- applicationBlocked: 27,
- hostCanceled: 145,
- applicationSelectionFailed: 40,
- removedCard: 41,
- collisionDetected: 42,
- referToHandheldDevice: 43,
- contactlessComplete: 44,
- requestSwitchToMSR: 45,
- wrongCardType: 46,
- noInterchangeProfile: 47
# MagTek - Setup
## tDynamo
### 1. Powering the device
To begin, power up the device by plugging it in. Each tDyanmo should come with a type-C USB cable included. Your device may come with an optional stand as well. Make sure to plug the cable into the SIDE of the device, NOT the base of the stand. The device will stay powered with a small battery, however, it will not stay charged for very long during regular use. When powered on, the device will make a BEEP, and a power indicator light will glow. The light can be green, yellow, or red depending on the battery status.

You may power the device OFF by holding the button on the side until you hear the DOUBLE BEEP. The button is on the right side of the device, under the magstripe reader. Pressing the same button will power the device back ON. 
### 2. Bluetooth
Your POS application will need to support bluetooth. The requirements for this depends on the mobile platform. [Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions), [iOS](https://developer.apple.com/documentation/bundleresources/information_property_list/nsbluetoothalwaysusagedescription)

The tDynamo works with a bluetooth varient called BLE or Bluetooth Low Energy. You can NOT use the host device's Bluetooth menu to connect to the card reader. Instead, the process for this will be handled in your POS application.

The FIRST time you ever connect to a device, you will need to put the device into PAIRING MODE. To do this, use the button on the side. Hold the button until you see a blue light flash on the device, then release the button. Remember that the pairing code for this device is: `000000`. 

In order to connect to a device in your application:
- Use the `startDeviceDiscovery` method to find nearby MagTek tDynamo devices
- Use the `name` value returned in the `startDeviceDiscovery` callback to call the `connect` method
- The FIRST time you connect to the device it will open a pairing menu. The password is `000000`
  - (The application may crash after first-time pairing. This is being looked into. You can restart the application to resume normal operation)
- When the device has been connected, the solid light will begin to blink slowly. You should receive a `true` value in the `connect` callback shortly after it begins to blink. 

Once a card reader is paired to the host device, you will see it show up in the Bluetooth menu. You can unpair devices from here.
### 3. Transactions
Currently SALE transactions are supported through our SDKs. To begin a SALE transaction, you need to supply a string representing a dollar value. some examples: `12.03`, `1.01`, `1.1`, `04.30`, `5` are all valid. 

Strings like `$1.01` are not allowed. do NOT place the dollar sign or any other currency symbol in the string. 

Supply the string to the `startTransaction` method. During the process of the transaction, the callback function will be passed:
- A message to show the card holder
- The last reported transaction event that happened
- The last reported transaction status of the device

You can see a list of the possible events and statuses [here](./magtek-card-reader.md)

When going through a transaction, these messages will be displayed to the card holder:
- 1. Transaction is started: `PRESENT CARD`
- 2. The card holder presents their card 
  - a. Card holder inserts card: `PLEASE WAIT`
  - b. Card holder taps card: `CARD READ OK, PLEASE REMOVE CARD`
- 3. If there was an error processing the card: `PROCESSING ERROR`
- 4. Transaction was approved: `APPROVED`
- 5. Transaction was declined or a processing error: `DECLINED`

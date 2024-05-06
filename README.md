# Triple Play Pay - Android MagTEK SDK
## Using the Library
To use this library in your project, place this in your pom.xml: (Double check that the version matches the most recent one available)
```xml
<dependency>
  <groupId>com.tripleplaypay.magteksdk</groupId>
  <artifactId>sdk-debug</artifactId>
  <version>0.6.0-rc.1</version>
</dependency>
```
Install the library with this command:
```sh
mvn install
```
## Building locally
The MagTek SDK is an Android Library. Running this command will produce `.aar` files for distribution under the folder `TPP-MagTekSDK/build/outputs/aar`
```
./gradlew :TPP-Android-MagTekSDK:build
```
Then, grab the file
```
cp ./TPP-Android-MagTekSDK/build/outputs/aar/TPP-Android-MagTekSDK-release.aar ./TPP-MagTekSDK.aar
```
# Documentation
Go [here](Docs/MagTekCardReader.md)

# Triple Play Pay - Android MagTEK SDK

## Building
The MagTek SDK is an Android Library. Running this command will produce `.aar` files for distribution under the folder `TPP-MagTekSDK/build/outputs/aar`
```
./gradlew :TPP-Android-MagTekSDK:build
```
Then, grab the file
```
cp ./TPP-Android-MagTekSDK/build/outputs/aar/TPP-Android-MagTekSDK-release.aar ./TPP-MagTekSDK.aar
```

## Publishing

```console
$ ./gradlew tasks --all | grep -i publish
Publishing tasks
TPP-Android-MagTekSDK:publish - Publishes all publications produced by this project.
TPP-Android-MagTekSDK:publishAllPublicationsToTppRepository - Publishes all Maven publications produced by this project to the Tpp repository.
TPP-Android-MagTekSDK:publishDebugPublicationToMavenLocal - Publishes Maven publication 'debug' to the local Maven repository.
TPP-Android-MagTekSDK:publishDebugPublicationToTppRepository - Publishes Maven publication 'debug' to Maven repository 'Tpp'.
TPP-Android-MagTekSDK:publishReleasePublicationToMavenLocal - Publishes Maven publication 'release' to the local Maven repository.
TPP-Android-MagTekSDK:publishReleasePublicationToTppRepository - Publishes Maven publication 'release' to Maven repository 'Tpp'.
TPP-Android-MagTekSDK:publishToMavenLocal - Publishes all Maven publications produced by this project to the local Maven cache.
TPP-Android-MagTekDemo:prepareLintJarForPublish
TPP-Android-MagTekSDK:prepareLintJarForPublish
```

# Documentation
Go (here)[Docs/MagTekCardReader.md]

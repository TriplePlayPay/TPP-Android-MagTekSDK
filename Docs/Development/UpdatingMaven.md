
# Publishing
First, don't forget to update github
```sh
git commit -am '<version>'
git push
```
There are two environments to publish to: `TppRepository` which is the live repository and `local` which is your local Maven repo. 
There are also two targets to build for: `debug` and `release`. 

Use this command to list all the publishing tasks. Run the one you need.
```sh
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
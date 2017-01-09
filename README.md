Wireless Vehicle Bus Adapter (WVA) Android Demo App
===================

The Wireless Vehicle Bus Adapter Android Demo App is an Android application
that uses the API of the Digi [Wireless Vehicle Bus Adapter][WVA], to show off certain
features of the WVA. This source has been contributed by 
[Digi International][Digi].

[Digi]: http://www.digi.com
[WVA]: http://www.digi.com/wva


Requirements and Setup
----------------------

 1. Ensure that [Android Studio](https://developer.android.com/sdk/installing/studio.html) is installed
 1. Clone this Git repository
 1. Import this project into Android Studio. Select the root directory of the repository
    (likely named "wva_android").
    - From the Quick Start screen: click Import Project...
    - From the menu bar: File -> Import Project...


Running the application's tests
-------------------------------

Before running the tests, ensure that you have a physical Android device connected to your computer,
or an Android emulator running within the computer.

#### Using Android Studio's test running facilities

1. From the menu bar, select Run -> Edit Configurations...
1. Click the green `+` icon and select "Android Tests"
1. Edit the Name field to "Demo App Tests"
1. In the Module dropdown, select "demo_app"
1. Set the Test value to "All in module"
1. Set the instrumentation runner to `com.digi.android.wva.WVATestRunner`
1. (Recommended) Select "Show chooser dialog" under Target Device
1. From the menu bar, select Run -> Run..., and choose "Demo App Tests"


#### Using Gradle (from Android Studio)

1. In Android Studio, open the Gradle view (menu bar -> View -> Tool Windows -> Gradle)
1. Within the Gradle view, double-click wva_android -> demo_app -> connectedCheck
1. Browse the test results by opening `demo_app/build/outputs/reports/androidTests/connected/index.html`
   in a web browser. You can also see code coverage information, under
   `demo_app/build/outputs/reports/coverage/debug`.
   
    - To find these files, you may need to open the repository's root directory in a file browser;
     Android Studio hides most of the `build` directory by default.

 
#### Using Gradle from the command line (advanced users)

These instructions assume that you have set up your environment (`JAVA_HOME`, `ANDROID_HOME`, `PATH`
variables) so that you can execute the Gradle wrapper from the command line, and that the working
directory of your shell is the root directory of the project (the directory containing `.gitignore`,
`gradlew`, `gradlew.bat`, etc.).

1. Run the `connectedCheck` task.
    - `.\gradlew.bat connectedCheck` on Windows, or `./gradlew connectedCheck` on Linux or OS X
1. Browse the test results by opening `demo_app/build/outputs/reports/androidTests/connected/index.html`
   in a web browser. You can also see code coverage information, under
   `demo_app/build/outputs/reports/coverage/debug`.
   
    - To find these files, you may need to open the repository's root directory in a file browser;
     Android Studio hides most of the `build` directory by default.
   

Compiling a release APK
-----------------------

In order to distribute an APK which can be installed on any compatible Android device, you
must first compile the APK and sign it with a release key.

#### Using Android Studio's app signing functionality

Follow the "Signing your App in Android Studio" instructions found
[here](http://developer.android.com/tools/publishing/app-signing.html#studio).

#### Using Gradle from the command line (advanced users)

These instructions assume that you have set up your environment (`JAVA_HOME`, `ANDROID_HOME`, `PATH`
variables) so that you can execute the Gradle wrapper from the command line, and that the working
directory of your shell is the root directory of the project (the directory containing `.gitignore`,
`gradlew`, `gradlew.bat`, etc.). We also assume that you have created or have access to a keystore
and a private key within that keystore.

1. Edit the file `demo_app/gradle.properties`, and specify the following values:

    - `releaseKeystore`: Path to the keystore containing the release key. (Absolute path, or relative to `demo_app/`)
    - `releaseKeystorePassword`: Password for the keystore
    - `releaseKeyAlias`: Alias of the private key to use
    - `releaseKeyPassword`: Password for the private key
    
1. Run the `assembleRelease` task.
    - `.\gradlew.bat assembleRelease` on Windows, or `./gradlew assembleRelease` on Linux or OS X
    
1. Find the release APK at `demo_app/build/outputs/apk/demo_app-release.apk`
   

License
-------

This software is open-source software. Copyright Digi International, 2014.

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this file,
You can obtain one at [http://mozilla.org/MPL/2.0/](http://mozilla.org/MPL/2.0/).

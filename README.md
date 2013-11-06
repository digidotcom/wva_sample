Wireless Vehicle Bus Adaptor (WVA) Android Sample App
===================

The Wireless Vehicle Bus Adaptor Android Sample App shows how to build an Android application 
that uses the API of the Digi Wireless Vehicle Bus Adaptor.  This source has been contributed by 
[Digi International][Digi].

  * The code shows how to access the API of the WVA. 

  * The code also shows how to graph vehicle speed and engine RPMs.

[Digi]: http://www.digi.com


Requirements and Setup
------------

Initial setup on computer:

1. Ensure Android SDK is installed, tools/ and platform-tools/ are on PATH
1a. Ensure Android API 17 is installed (`android list targets' should include
    an "android-17" target)
2. Ensure Ant is installed
3. cd into root directory of wva_android repository
4. cd into WVA_App directory
5. Run:
    android update project --path . --subprojects
6. cd back into root directory
7. Run:
    android update test-project -m ../WVA_App -p WVA_App_Test
8. Run:
    android update lib-project -t "android-17" -p ActionBarSherlock
    android update lib-project -t "android-17" -p WVALib

9. Presumably, to prepare for library testing, you'll do something like this:
    android update test-project -m ../WVALib -p WVALib_Test

Building the application during development
------------

Note: It is highly recommended that you use your development IDE (Eclipse, or Android Studio) to 
build and install the WVA sample app during development, as an IDE greatly reduces the complexity 
of the process. See the Android developer documentation for using your IDE for more information.

From the command line:

1. If developing against an Android emulator, start the emulator and ensure it is the
   only device connected via ADB (run `adb devices`). If developing on an Android device,
   plug it in via USB and check connectivity using `adb devices`.
2. Change directory to WVA_App
3. Run the following command:
        ant clean debug install
- This will clean up build-related files, build the app in debug mode, and install it on
  the connected device/emulator.

Building the application for release:
------------


Note: It is highly recommended that you use your development IDE (Eclipse, or Android Studio) to 
build and sign the WVA sample app for release, as an IDE greatly reduces the complexity of the 
process (keeping track of keystores, passwords, etc.). See the Android developer documentation 
for using your IDE for more information.

Before you can build the application for release, you must have a suitable private key (and 
keystore) available on your computer. See the following URL: 
http://developer.android.com/tools/publishing/app-signing.html#cert 
for more information on application signing.

From the command line:

1. Change directory to WVA_App
2. Modify the local.properties file, uncommenting the lines beginning with 'key.',
    and filling in the following information there:
    - key.store: Path to the keystore you will be signing the application with
    - key.alias: Alias for the key you will be signing the application with
    - key.store.password: Password for the keystore
    - key.alias.password: Password for the key
3. Run the following command:
        ant clean release
- This will clean up build-related files and build the app in release mode,
    signing it with the key information provided in local.properties
4. The signed APK can be found at bin/WVA_Sample_App-release.apk


License
-------

This software is open-source software.  Copyright Digi International, 2013.

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this file,
You can obtain one at http://mozilla.org/MPL/2.0/.

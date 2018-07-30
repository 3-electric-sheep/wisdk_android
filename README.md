# WiSDK

## About

This Wi SDK allows integration of native mobile apps with the Welcome Interruption (Wi) servers.  It allows for the collecting
location information from a mobile device to the Wi Servers and allow the receipt of notifications from Wi servers.

The SDK also provides various interfaces to the REST api suported by the Wi servers.

The SDK is available for IOS and Android and is available an:-

* Objective C library (IOS)
* Java library (Android)
* React Native library (IOS + Android)

This document specifically for the Android Java version of the library

## Requirements

Currently the WiSDK has the following requirements:-

* Android Version 5.0 Lollipop or higher
* Android Studio 3.1+

It also requires, the following libraries as dependencies

* Volley network library
* Google Play Sevices
* Google Firebase Cloud Services

## SDK Installation and Setup
There are a few important steps to get out of the way when integrating Wi with your app for the first time.
Follow these instructions carefully to ensure that you will have a smooth development experience.


### Git Submodule library
The WiSDK library is distributed in souce code form and can be added to an existing library using a git submodule.

To add the WiSDK to an Android application goto the root of the app folder tree and via a terminal window type in the following command

```bash
git submodule add https://3es-Integrator:3zrUfjvVBW@github.com/3-electric-sheep/wisdk_android wisdk
```

This will add the library in source code form to your application.

***NOTE**: if you are cloning the example repository or anything that has submodules, some older versions of 
git require the --recurse-submodules option*

```bash
git clone --recurse-submodules
```

### Install FCM 

Install googles Firebase cloud messaging (FCM)as described in the documentation here:-

https://firebase.google.com/docs/cloud-messaging/android/client

Ensure that a valid google-service.json file is available at the top level of your app.

***NOTE**: WiSDK shares many of the dependancies FCM .  It is very important that
all the play services dependancies are the same version and all the FCM dependancies match. It is safe
to update the WiSDK dependances to match the version of FCM to use.

### App level build.gradle file

In the app level build.gradle file add the following to the dependancies section:

```
implementation project(":wisdk")

// for wisdk
implementation "com.google.android.gms:play-services-base:15.0.1"
implementation "com.google.android.gms:play-services-location:15.0.1"
implementation "com.google.android.gms:play-services-wallet:15.0.1"


// Firebase dependencies
implementation "com.google.firebase:firebase-core:16.0.1"


// Firebase cloud messaging plus badge support
implementation "com.google.firebase:firebase-messaging:17.1.0"
```

at the bottom of the file add


```gradle
apply plugin: 'com.google.gms.google-services'
```

***NOTE:** the WiSDK compileSdkVersion and targetSdkVersion is set to 26. This will soon be the minimum level
that apps published on the Play store can use.
                     
                     
### Top Level build.gradle

In the top level build.gradle file ensure that the google and jcenter repositories are specified and 
that the google services is in the classpath

```
buildscript {
    
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.1.3'
        classpath 'com.google.gms:google-services:4.0.1'
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
```

### WiSDK Level build.gradle

This is ready to go. You man need to change the versions of playservers and FCM dependancies if your app has different versions. You
could also add a resolution section at the app level build gradle to force particular versions of playservices and FCM.

```
    configurations.all {
        resolutionStrategy.eachDependency { DependencyResolveDetails details ->
            if (details.getRequested().getGroup() == 'com.google.android.gms') {
                details.useVersion("15.0.1")
            }
        }
    }

```

### Permission Setup
While you are setting up your development and production apps, you need to setup the appropriate permissions in your manifest file.
THe following persmissons are required:-

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

These are already setup in the WiSDK android manifiest.

## Getting Started

Welcome interruption is a platform for real time digital reach. By using the WiSDK you can turn any mobile application into an
effective mobile marketting tool that can target customers based on their real time location.

To get started we need to explain some terminoloy. In Welcome Interruption there are a number of key entities:-

 - Provider - A provider is the main entity which controls which offers gets sent to where, when and who. This is usually the owner of
 the users of the system (ie. the company that owns the app the Wi Sdk will be added too)
 - Place of interest (POI) - A defined geographic area that the provider sets up.
 - Event - A real time offer created by a Provider that is targeted to one or more POI's that they previously created
 - Users - A customer of the provider. Users can be anonymous and have a link to an external system or they can be full users (ie. email, name, etc)
 - Device - Links a phone and user to a provider. Contains reference to a user, current Lat/Lng of a phone as well as preferred
 notification mechanisms.
 - Campaign - the ability to target users/devices using external attributes as well as geo information.


Typically to setup a Client in Welcome Interruption we do the following:-

1. add a provider to Welcome interruption and configure it with a campaign schema, external system intergration details and push
certificates and API keys. This will require us to have your clients APNS push cerificates and any API key for Googles FCM serivce.
2. Once the provider is setup you will be provided with a provider key and certificate keys which **MUST** be specified as
part of the WiSDK configuration.
3. Start Integrating the WiSDK

## WiSDK integration

Typically integration is done as follows:-

1. Configure SDK
2. Create listener / delegate (optional)
3. Start Wi up
4. Use the API to list offers, update profiles, etc (optional)
5. Permissions and capabilities

Wi works silently in background sending location updates to our servers as users go about their daily business. You can even close the app and
everything just keeps working

A minimal integration is just include the TesWIApp and TesConfig classes then adding code to the onCreate method in the Main Activity class.

```java
import com.welcomeinterruption.wisdk.TesWIApp;
import com.welcomeinterruption.wisdk.TesConfig;
 
public class MainActivity extends AppCompatActivity  {
     private static final String TAG = MainActivity.class.getSimpleName();
     private static final String PROVIDER_KEY = "5b53e675ec8d831eb30242d3";
     
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);
 
         TesWIApp.createManager(this, this, R.layout.activity_main);
         TesConfig config = new TesConfig(PROVIDER_KEY);
 
         config.authAutoAuthenticate = true;
         config.deviceTypes = TesConfig.deviceTypeGCM | TesConfig.deviceTypeWallet;
         try {
             config.authCredentials = new JSONObject();
             config.authCredentials.put("anonymous_user", true);
         }
         catch (JSONException e){
             Log.e(TAG, "Failed to create authentication details: "+e.getLocalizedMessage());
         }
 
         config.testPushProfile = "wisdk-example-fcm";
         config.pushProfile = "wisdk-example-fcm";
 
         TesWIApp app = TesWIApp.manager();
         app.listener = this;
         app.start(config);
     }
}
```

### configure

Configuration is done completely through the TESConfig object.  It is used to bind a provider with an app and describe how the WiSDK should
interact with the device and Wi Servers.

Typically a config object is created at app startup and then passed to the TESWIApp object start method. The config object can set the
sensitivty of geo regions monitored, how users and devices are created and the type of notification mechanism that should be used by the sdk


```java
     TesConfig config = new TesConfig(PROVIDER_KEY);

     config.authAutoAuthenticate = true;
     config.deviceTypes = TesConfig.deviceTypeGCM | TesConfig.deviceTypeWallet;
     try {
         config.authCredentials = new JSONObject();
         config.authCredentials.put("anonymous_user", true);
         config.authCredentials.put("external_id", ”1234567890”);  // external system user/member id)
     }
     catch (JSONException e){
         Log.e(TAG, "Failed to create authentication details: "+e.getLocalizedMessage());
     }
     config.testPushProfile = "wisdk-example-fcm"; // test profile name (allocted by 3es)
     config.pushProfile = "wisdk-example-fcm"; // prod profile name (allocated by 3es

```


### Listeners

The WiSDK supports a interface or listener class that can be used to get information about what the WiSDK is doing behind the scenes.
Implmenting this interface is optional but may be useful depending on the app you are integrating with.

this protocol is defined in TESWiApp as follows:-

```java
/**
 * Listener interface used to tell the host of interesting things that may happen.
 */
public interface TesWIAppListener {

    /**
     * sent when authorization has failed (401)
     *
     * @param statusCode the HTTP status code
     * @param data Response body
     * @param notModified True if the server returned a 304 and the data was already in cache
     * @param networkTimeMs Round-trip network time to receive network response
     * @param headers map of headers returned with this response, or null for none
     **/
    void authorizeFailure(int statusCode,  byte[] data,  boolean notModified, long networkTimeMs, Map<String, String> headers);

    /**
     * sent when authorization is complete
     *
     * @param status         TESCallStatus value
     * @param responseObject JSONObject response object from call
     * @param error          TesApiException set on error or nill
     **/
    void onAutoAuthenticate(int status, @Nullable JSONObject responseObject, @Nullable TesApiException error);

    /**
     * sent when a new access token is returned
     */
    void newAccessToken(@Nullable String token);

    /**
     * sent when a new device token has been created
     */
    void newDeviceToken(@Nullable String token);

    /**
     * sent when a new push token is returned
     */
    void newPushToken(@Nullable String token);

    /**
     * Called when a remmote notification needs to be processed
     */
    void onRemoteNotification(@Nullable JSONObject data);


    /**
     * Called when a remmote notification needs to be processed
     */
    void onRemoteDataNotification(@Nullable JSONObject data);


    /**
     * Called when a remmote notification needs to be processed
     */
    void onWalletNotification(@Nullable JSONObject data);

    /**
     * Called when remote notifications is registered or fails to register
     *
     * @param token the new token
     */
    void onRefreshToken(@NonNull String token);

    /**
     * Called when a wallet object is saved to the wallet
     *
     * @param  requestCode the code of the request
     * @param  resultCode the result code.
     * @param  data extra stuff with the save to wallet
     * @param  msg description of return code
     *
     */
    void saveWallet(int requestCode, int resultCode, Intent data, String msg);
}
```

If defined it is typically added to the MainActivity or other top level compnent then assigned to the listener property of the TesWiApp class
```java
 public class MainActivity extends AppCompatActivity implements TesWIApp.TesWIAppListener {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // initial config etc...
            
            TesWIApp app = TesWIApp.manager();
            app.listener = this; //<= setup listener to be this class
            app.start(config);
        }
        
        @Override
        public void authorizeFailure(int statusCode, byte[] data, boolean notModified, long networkTimeMs, Map<String, String> headers) {
            Log.i(TAG, String.format("-->authorizeFailure: %d", statusCode));
        }
        
        // etc...
 }
```

### Start Wi up

The Wi SDK revolves around the TESWIApp singleton object. It is created once for an app and is used to for location monitoring, authentication with Wi servers,
notification management and all sorts of communication to/from the Wi Servers.

```java
    TesWIApp app = TesWIApp.manager();
    app.listener = this; // only if TesWIAppListener is specified
    app.start(config);
```

NOTE: start asks for necessary permissions, registers FCM push tokens and uses https to authenticate and communicate with the WI servers. It is asyncrohnous and
in nature.


### Using thie API

The remainder of the WiSDK wraps the Wi Rest based API. This API can be used to

* view live/historical events
* events that have been taken up by this user
* setting up inclusions/exclusions for event notification
* searching for events

### Permissions and capabilites


## API documentation

For further API documentation, clone the repo and open the doc/html/index.html file

## Example

To run the example project, clone the wisdk_android_example repo, compile and run it. This example is useful as it has a
project correctly configured to run Wi.

It is a bare bones project that will send location information from the device to the Wi Servers.  It also
has a demo provider key  which can be used to send offers from wi to the device.

## Author

Welcome Interruption and the WiSDK are developed by the 3-electric-sheep pty ltd.

for support please contact:-

pfrantz, pfrantz@3-elecric-sheep.com

## License

WiSDK is available under the Welcome Interruption SDK License. See the LICENSE file for more info.


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

### FCM and Location Permission Setup
While you are setting up your development and production apps, you need to configure them for push services.
For detailed instructions on this process, please see the APNS Setup documentation. The APNS certificates must also be
converted to PEM format and setup on the Wi servers. (this is usually part of the client setup on Wi servers)

Wi will ask for "Always and when in use location permissions" so that it can collect location information while the app is not in use.
You will  need to specify a description for NSLocationAlwaysAndWhenInUseUsageDescription in your info.plist to enable the asking for this permission.

NOTE: NSLocationWhenInUseUsageDescription can be specified if you don't want to allow such location gathering in backgroun
but this will serverly limit what Wi is capable of.

**IMPORTANT: do *NOT* set the background location checkbox on the background capabilities option.
Wi does not use it and setting this will adversely affect battery life.**

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

A minimal integration is just include the TESWiApp.h header and adding code to the didFinishLaunchingWithOptions method in the app delegate.

```objective-c
#import "WiAppDelegate.h"
#import "WiSDK/TESWIApp.h"

@implementation WiAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    // the provider key this application is tied too (provider to you by 3es)
    NSString * PROVIDER_KEY = @"5b53e675ec8d831eb30242d3";

    // first thing to do is create a configuration object and overwrite values to suit your environment.
    // most defaults are sensible.
    TESConfig * config = [[TESConfig alloc] initWithProviderKey:PROVIDER_KEY];
    config.authAutoAuthenticate = YES;
    config.authCredentials = @{
           @"anonymous_user": @YES,
    };
    config.deviceTypes = deviceTypeAPN | deviceTypeWallet;

    // IOS has the concept of production and developement APNS servers - we need to ensure the right keys
    // are setup if we are running in debug mode or production mode.
#ifdef DEBUG
    config.testPushProfile = @"wisdk-example-aps-dev";  // test profile name (allocted by 3es)
    config.pushProfile = @"wisdk-example-aps-dev"; // prod profile name (allocated by 3es)
#endif
#ifndef DEBUG
    config.testPushProfile = @"wisdk-example-aps-prod";  // test profile name (allocted by 3es)
    config.pushProfile = @"wisdk-example-aps-prod"; // prod profile name (allocated by 3es)
#endif

    // next we create our singleton app object
    TESWIApp * app = [TESWIApp manager];

    // if we have a delegate listener class set it up here
    app.delegate = self;

     // start things up - this will setup push notificatons, setup fg and bg location montioring and authenticate
     // with Wi servers.
    [app start:config withLaunchOptions:launchOptions];

    return YES;
}
```

### configure

Configuration is done completely through the TESConfig object.  It is used to bind a provider with an app and describe how the WiSDK should
interact with the device and Wi Servers.

Typically a config object is created at app startup and then passed to the TESWIApp object start method. The config object can set the
sensitivty of geo regions monitored, how users and devices are created and the type of notification mechanism that should be used by the sdk


```objective-c
    TESConfig * config = [[TESConfig alloc] initWithProviderKey:PROVIDER_KEY];
    config.authAutoAuthenticate = YES;
    config.authCredentials = @{
           @"anonymous_user": @YES,
           @"external_id:: ”1234567890”,  // external system user/member id
    };
    config.deviceTypes = deviceTypeAPN | deviceTypeWallet;
    config.testPushProfile = @"wisdk-example-aps-prod";  // test profile name (allocted by 3es)
    config.pushProfile = @"wisdk-example-aps-prod"; // prod profile name (allocated by 3es

```


### Listeners

The WiSDK supports a protocol or listener class that can be used to get information about what the WiSDK is doing behind the scenes.
Implmenting this protocol is optional but may be useful depending on the app you are integrating with.

this protocol is defined in WiSDK/TESWiApp.h as follows:-

```objective-c

 @protocol TESWIAppDelegate <NSObject>

    /*
     sent when authorization has failed (401)
    */
    - (void) authorizeFailure: (NSInteger) httpStatus;

    @@optional

    /*
     sent when authorization is complete
    */
    - (void) onAutoAuthenticate:(TESCallStatus)status withResponse: (nonnull NSDictionary *) responseObeject;

    /*
     sent when a new access token is returned
    */
    - (void) newAccessToken: (nullable NSString *) token;

    /*
     sent when a new device token has been created
     */
    - (void) newDeviceToken: (nullable NSString *) token;

    /*
      sent when a new push token is returned
     */
    - (void) newPushToken: (nullable NSString *) token;

    /*
      Called when a remmote notification needs to be processed
     */
    - (void) processRemoteNotification: (nullable NSDictionary *) userDictionary;

    /*
      Called when remote notifications is registered or fails to register
     */
    -(void) onRemoteNoficiationRegister:(TESCallStatus)status withResponse: (nonnull NSDictionary *) responseObeject;

 @end

```

### Start Wi up

The Wi SDK revolves around the TESWIApp singleton object. It is created once for an app and is used to for location monitoring, authentication with Wi servers,
notification management and all sorts of communication to/from the Wi Servers.

```objective-c

    TESWIApp * app = [TESWIApp manager];
    app.delegate = self; // Only needed if you implement the listener protocol
    [app start:config withLaunchOptions:launchOptions];
```

NOTE: start asks for necessary permissions, registers push tokens and uses https to authenticate and communicate with the WI servers. It is asyncrohnous and
in nature.

### Using thie API

The remainder of the WiSDK wraps the Wi Rest based API. This API can be used to

* view live/historical events
* events that have been taken up by this user
* setting up inclusions/exclusions for event notification
* searching for events

### Permissions and capabilites

Ensure that your info.plist has the correct location privacy entries

```xml
    <key>
        NSLocationAlwaysUsageDescription
    </key>
    <string>
        We use your location to notify you of great offers near you. Please select always as this gives you welcome interuptions near you.
    </string>
    <key>
        NSLocationAlwaysAndWhenInUseUsageDescription
    </key>
    <string>
        We use your location to notify you of great offers near you. Please select always as this gives you welcome interuptions near you.
    </string>
    <key>
       NSLocationWhenInUseUsageDescription
    </key>
    <string>
        We use your location to notify you of great offers near you. Please select always as this gives you welcome interuptions near you.
    </string>
```

Ensure that in the capabilites you have

* Push Notifications enabled
* Wallet services enabled (optional if you want to support wallet services)
* Background services - and tick allow remote notifications

** NOTE: do NOT tick allow background location as this will drain battery very quickly.

## API documentation

For further API documentation, clone the repo and open the doc/html/index.html file

## Example

To run the example project, clone the repo, and run `pod install` from the Example directory first. This example is useful as it has a
project correctly configured to run Wi.

It is a bare bones project that will send location information from the device to the Wi Servers.  It also
has a demo provider key  which can be used to send offers from wi to the device.

## Author

Welcome Interruption and the WiSDK are developed by the 3-electric-sheep pty ltd.

for support please contact:-

pfrantz, pfrantz@3-elecric-sheep.com

## License

WiSDK is available under the Welcome Interruption SDK License. See the LICENSE file for more info.

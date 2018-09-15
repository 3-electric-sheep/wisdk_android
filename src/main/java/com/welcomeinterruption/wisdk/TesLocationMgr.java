//  Created by Phillp Frantz on 13/07/2017.
//  Copyright © 2012-2018 3 Electric Sheep Pty Ltd. All rights reserved.
//
//  The Welcome Interruption Software Development Kit (SDK) is licensed to you subject to the terms
//  of the License Agreement. The License Agreement forms a legally binding contract between you and
//  3 Electric Sheep Pty Ltd in relation to your use of the Welcome Interruption SDK.
//  You may not use this file except in compliance with the License Agreement.
//
//  A copy of the License Agreement can be found in the LICENSE file in the root directory of this
//  source tree.
//
//  Unless required by applicable law or agreed to in writing, software distributed under the License
//  Agreement is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
//  express or implied. See the License Agreement for the specific language governing permissions
//  and limitations under the License Agreement.
package com.welcomeinterruption.wisdk;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;


import org.json.JSONException;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.tasks.OnCompleteListener;


public class TesLocationMgr {
    private static final String TAG = "TESLocationMgr";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Config for the system
     */
    private final TesConfig config;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;


    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Context to the main activity that all the intents will be bound too
     */
    private Context mCtx;


    /**
     * Constructor for class
     *
     * @param ctx - the context (is the fragment)
     * @param config - the config object
     */


    public boolean requireBackgroundProcessing = true;

    public TesLocationMgr(Context ctx, TesConfig config) {
        this.config = config;
        this.mCtx = ctx;
        this.mFusedLocationClient = null;
    }

    public void connect(boolean requireBackgroundProcessing) {
        this.requireBackgroundProcessing = requireBackgroundProcessing;
        // get fused loc client and ask for latest pos
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this.mCtx);
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        // Note: apps running on "O" devices (regardless of targetSdkVersion) may receive updates
        // less frequently than this interval when the app is no longer in the foreground.
        mLocationRequest.setInterval(this.config.locUpdateInterval);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(this.config.locFastestUpdateInterval);

        mLocationRequest.setPriority(this.config.locPriorityAccuracy);

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        mLocationRequest.setMaxWaitTime(this.config.locMaxWaitTime);
    }

    private PendingIntent getLocationUpdatesPendingIntent(Context ctx) {
        // Note: for apps targeting API level 25 ("Nougat") or lower, either
        // PendingIntent.getService() or PendingIntent.getBroadcast() may be used when requesting
        // location updates. For apps targeting API level O, only
        // PendingIntent.getBroadcast() should be used. This is due to the limits placed on services
        // started in the background in "O".

        Intent intent = new Intent(ctx, TesLocationUpdatesBroadcastReceiver.class);
        intent.setAction(TesLocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * gets last known location if any
     */
    @SuppressWarnings("MissingPermission")
    public void getLastKnownLocation(OnCompleteListener<android.location.Location> listener) {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(listener);

    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    @SuppressWarnings("MissingPermission")
    public void requestLocationUpdates(OnCompleteListener<Void> listener) {
        Log.i(TAG, "Starting location updates");
        TesUtils.setRequestingLocationUpdates(this.mCtx, true);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, getLocationUpdatesPendingIntent(this.mCtx))
                .addOnCompleteListener(listener);

    }

    /**
     * requests removal of location updates.
     */
    public void removeLocationUpdates(OnCompleteListener<Void> listener) {
        Log.i(TAG, "Removing location updates");
        mFusedLocationClient.removeLocationUpdates(getLocationUpdatesPendingIntent(this.mCtx))
                .addOnCompleteListener(listener);
    }
}





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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.common.api.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TesLocationMgr implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    //----------------------------------------------
    // TesLocationMgrListener interface
    //----------------------------------------------

    public interface TesLocationMgrListener {
        void sendDeviceUpdate(@NonNull TesLocationInfo locInfo, boolean inBackground, final TesApi.TesApiListener listener) throws JSONException;

        void onConnected(@Nullable Bundle bundle);

        void onConnectionSuspended(int i);

        void onConnectionFailed(@NonNull ConnectionResult connectionResult);

        void onError(@Nullable String msg);
    }

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
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;


    /** the id of the current geofence */
    private String geoId = null;

    /** are we suspeneded
     */
    private boolean suspended = false;

    /**
     * Context to the main activity that all the intents will be bound too
     */
    private Context mCtx;

    /**
     * the main activity fragment
     */
    private FragmentActivity mFragmentActivity;

    /**
     * the root view id to anhcor snackviews too
     */
    private int mViewId;

    /**
     * The listener object that gets sent device updates
     */
    public @Nullable TesLocationMgrListener listener = null;

    /**
     * the last location returned
     */

    public @Nullable TesLocationInfo lastLocation = null;
    /**
     *  Constructor for class
     * @param activity - the fragment that starts the locaiton createManager
     * @param ctx - the context (is the fragment)
     * @param config - the config object
     */


    public TesLocationMgr(FragmentActivity activity, Context ctx, int viewId,  TesConfig config) {
        this.config = config;
        this.mFragmentActivity = activity;
        this.mCtx = ctx;
        this.mViewId = viewId;
        this.listener = null;
        this.geoId = null;
    }

    public boolean start(boolean requireBackgroundProcessing) {
        PreferenceManager.getDefaultSharedPreferences(this.mCtx)
                .registerOnSharedPreferenceChangeListener(this);

        // Check if the user revoked runtime permissions.
        if (!hasPermissions()) {
            requestPermissions();
            return false;
        }

        buildGoogleApiClient();
        return true;
    }

    public void stop() {
        PreferenceManager.getDefaultSharedPreferences(this.mCtx)
                .unregisterOnSharedPreferenceChangeListener(this);

        if (this.mGoogleApiClient != null && (this.mGoogleApiClient.isConnected() || this.mGoogleApiClient.isConnecting())){
            this.mGoogleApiClient.disconnect();
        }
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

    /**
     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
     * int, GoogleApiClient.OnConnectionFailedListener)}. I.e., GoogleApiClient connects in
     * {@link AppCompatActivity#onStart}, or if onStart() has already happened, it connects
     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
     */
    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
/*        if (mFragmentActivity != null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this.mCtx)
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this.mFragmentActivity, this)
                    .addApi(LocationServices.API)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        else {*/
            mGoogleApiClient = new GoogleApiClient.Builder(this.mCtx)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();

/*        }*/
        createLocationRequest();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        this.suspended = false;
        if (this.listener != null)
            this.listener.onConnected(bundle);

    }

    @Override
    public void onConnectionSuspended(int i) {
        final String text = "Connection suspended";
        Log.w(TAG, text + ": Error code: " + i);
        this.suspended = true;
        if (this.listener != null)
            this.listener.onConnectionSuspended(i);
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        final String text = "Exception while connecting to Google Play services";
        Log.w(TAG, text + ": " + connectionResult.getErrorMessage());
        if (this.listener != null)
            this.listener.onConnectionFailed(connectionResult);
    }


    private PendingIntent getPendingIntent(Context ctx) {
        // Note: for apps targeting API level 25 ("Nougat") or lower, either
        // PendingIntent.getService() or PendingIntent.getBroadcast() may be used when requesting
        // location updates. For apps targeting API level O, only
        // PendingIntent.getBroadcast() should be used. This is due to the limits placed on services
        // started in the background in "O".

        // TODO(developer): uncomment to use PendingIntent.getService().
//        Intent intent = new Intent(this, TesLocationUpdatesIntentService.class);
//        intent.setAction(TesLocationUpdatesIntentService.ACTION_PROCESS_UPDATES);
//        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent = new Intent(ctx, TesLocationUpdatesBroadcastReceiver.class);
        intent.setAction(TesLocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this.mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Return the current state of the permissions needed.
     */
    public boolean hasPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this.mCtx,
                this.config.locPermission);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (this.mFragmentActivity == null)
            return; // no use as we don't have a ui

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this.mFragmentActivity,
                        this.config.locPermission);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    this.mFragmentActivity.findViewById(this.mViewId),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(TesLocationMgr.this.mFragmentActivity,
                                    new String[]{config.locPermission},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this.mFragmentActivity,
                    new String[]{config.locPermission},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Should be called in the Callback received when a permissions request has been completed.
     */
    public void processRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Kick off the process of building and connecting
                // GoogleApiClient.
                buildGoogleApiClient();
            } else {
                // Permission denied.
                showPermissionDenied();
            }
        }
    }

    private void showPermissionDenied() {

        // Notify the user via a SnackBar that they have rejected a core permission for the
        // app, which makes the Activity useless. In a real app, core permissions would
        // typically be best requested during a welcome-screen flow.

        // Additionally, it is important to remember that a permission might have been
        // rejected without asking the user for permission (device policy or "Never ask
        // again" prompts). Therefore, a user interface affordance is typically implemented
        // when permissions are denied. Otherwise, your app could appear unresponsive to
        // touches or interactions which have required permissions.
        Snackbar.make(
                mFragmentActivity.findViewById(this.mViewId),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mFragmentActivity.startActivity(intent);
                    }
                })
                .show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(TesUtils.KEY_LOCATION_UPDATES_RESULT)) {
            //TODO: pjf - do something mLocationUpdatesResultView.setText(TesUtils.getLocationUpdatesResult(this));
        } else if (s.equals(TesUtils.KEY_LOCATION_UPDATES_REQUESTED)) {
            //TODO: pjf - do something updateButtonsState(TesUtils.getRequestingLocationUpdates(this));
        }
    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    public void requestLocationUpdates() {
        try {
            Log.i(TAG, "Starting location updates");
            TesUtils.setRequestingLocationUpdates(this.mCtx, true);
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, getPendingIntent(this.mCtx));
        } catch (SecurityException e) {
            Log.e(TAG, "Request location updates failed - invalid permission");
            e.printStackTrace();
        }
    }

    /**
     * Handles the Remove Updates button, and requests removal of location updates.
     */
    public void removeLocationUpdates(Context ctx) {
        Log.i(TAG, "Removing location updates");
        TesUtils.setRequestingLocationUpdates(this.mCtx, false);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
                getPendingIntent(ctx));
    }

    /**
     * Sends last known location if any
     * @return
     */
    public void sendLastKnownLocation() {
        if (!hasPermissions()){
            return;
        }
        try {
            Location lastloc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastloc != null) {
                try {
                    Log.i(TAG, String.format("Last known location: (%s %s)", lastloc.getLatitude(), lastloc.getLongitude()));
                    TesJobMgr jm = new TesJobMgr(this.mCtx, this.config);

                    List<Location> locations = new ArrayList<Location>();
                    locations.add(lastloc);

                    this.lastLocation = new TesLocationInfo(lastloc, false);

                    JSONArray locArray = TesUtils.locationsToJson(locations, true);
                    JSONObject params = new JSONObject();
                    params.put(TesDeviceUpdateService.PARAM_LOC_LIST, locArray);

                    jm.scheduleJob(TesDeviceUpdateService.class, this.config.delay,this.config.deadline, this.config.networkType, this.config.requireIdle, this.config.requireCharging, params);

                    this.addGeofence(this.lastLocation.latitude, this.lastLocation.longitude);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to schedule location update. invalid JSON found");
                    e.printStackTrace();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Request location updates failed - invalid permission");
            e.printStackTrace();
        }
    }

    public boolean ensureMonitoring()
    {
        if (mGoogleApiClient == null) {
            if (hasPermissions()) {
                buildGoogleApiClient();
                return true;
            }
        }
        else {
            if (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())
                return true;
            else {
                mGoogleApiClient.connect();
                return true;
            }
        }
        return false;
    }

    public String getGeoId(){
        return this.geoId;
    }

    public  boolean isConnected(){
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    public  boolean isConnecting(){
        return (mGoogleApiClient != null && mGoogleApiClient.isConnecting());
    }


    public boolean isSuspended() {
        return (mGoogleApiClient != null && suspended);
    }
    @SuppressLint("MissingPermission")
    private void startMornitoringGeofence(Geofence geofenceToAdd){
        if (!hasPermissions()){
            return;
        }

        // 1. Create an IntentService PendingIntent
        Intent intent = new Intent(mCtx, TesGeofenceTransitionsIntentService.class);
        PendingIntent pendingIntent =
        PendingIntent.getService(mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // 2. Associate the service PendingIntent with the geofence and call addGeofences
        GeofencingRequest gfr = buidlGeofencingRequest(geofenceToAdd);
        PendingResult<Status> result =
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, gfr, pendingIntent);

        // 3. Implement PendingResult callback
        result.setResultCallback(new ResultCallback<Status>(){
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                } else {
                    String msg = String.format("Adding geofence failed: %s", status.getStatusMessage());
                    Log.e(TAG, msg);
                    sendError(msg);
                }
            }
        });
    }

    private void stopMonitoringGeofence(){

        // 1. Create a list of geofences to remove
        List<String> removeIds = new ArrayList<>();
        removeIds.add(this.geoId);

        // 2. Use GoogleApiClient and the GeofencingApi to remove the geofences
        PendingResult<Status> result = LocationServices.GeofencingApi.removeGeofences(this.mGoogleApiClient, removeIds);
        result.setResultCallback(new ResultCallback<Status>() {

            // 3. Handle the success or failure of the PendingResult
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    geoId = null;
                } else {
                    String msg = String.format("Removing geofence failed: %s" , status.getStatusMessage());
                    Log.e(TAG, msg);
                    sendError(msg);
                }
            }
        });
    }

    private GeofencingRequest buidlGeofencingRequest(Geofence geofenceToAdd) {
        List<Geofence> geofencesToAdd = new ArrayList<>();
        geofencesToAdd.add(geofenceToAdd);
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(this.config.geoInitialTrigger);
        builder.addGeofences(geofencesToAdd);
        return builder.build();
    }

    public Geofence buildGeofence(double latitude, double longitude)
    {
        geoId = UUID.randomUUID().toString();
        return new Geofence.Builder()
                .setRequestId(geoId)
                .setTransitionTypes(this.config.geoTransitionType)
                .setCircularRegion(latitude, longitude, this.config.geoRadius)
                .setExpirationDuration(this.config.geoExpiry)
                .setLoiteringDelay(this.config.geoLoiteringDelay)
                .build();
    }

    public void addGeofence(double latitude, double longitude)
    {
        if (this.geoId != null)
            this.removeGeofence();
        Geofence geoToAdd = buildGeofence(latitude,longitude);
        startMornitoringGeofence(geoToAdd);

    }

    public void removeGeofence(){
        if (this.geoId == null)
            return;

        stopMonitoringGeofence();
    }

    private void sendError(String error){
        if (this.listener != null)
            this.listener.onError(error);
    }
}


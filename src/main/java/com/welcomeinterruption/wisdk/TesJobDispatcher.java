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

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobService;

import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pfrantz on 2/6/18.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */
public class TesJobDispatcher {
    private static String TES_JOBPARAM_CONFIG= "TesConfig";

    private static String TES_JOBPARAM_SUCCESS = "success";
    private static String TES_JOBPARAM_MSG = "msg";
    private static String TES_JOBPARAM_CODE = "code";
    private static String TES_JOBPARAM_DATA = "data";

    public static String TES_KEY_LOCATIONS = "locations";
    public static String TES_KEY_LASTLOCATION = "lastLocation";

    public static String TES_KEY_TRIGGERING_GEOFENCES = "triggeringGeofences";
    public static String TES_KEY_TRIGGERING_LOCATION = "triggeringLocation";
    public static String TES_KEY_GEOFENCE_TRANSITION = "geofenceTransition";

    public static int ERROR_JSON_ENCODE_DECODE = -3;


    private static final String TAG = "TesJobDispatcher";

    private FirebaseJobDispatcher dispatcher = null;
    private TesConfig config = null;

    private static int mJobId = 0;

    private static synchronized int nextJobId() {
        return ++mJobId;
    }


    public TesJobDispatcher(Context context, TesConfig config) {
        this.dispatcher  = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        this.config = config;
    }

    /**
     * Sets the config field in bundle and returns the bundle
     *
     * @param b = Bundle to add config too
     */
    public  Bundle setConfig(Bundle b) throws JSONException {
        if (this.config == null)
            return b;

        b.putString(TES_JOBPARAM_CONFIG, this.config.toJSON().toString());
        return b;
    }

    /**
     * Set an error in the bundle to pass to the backend
     * @param msg - message to set
     * @param code - error code
     * @return bundle
     */
    public static Bundle setError(String msg, int code){
        Bundle b = new Bundle();
        b.putInt(TES_JOBPARAM_SUCCESS, 0);
        b.putString(TES_JOBPARAM_MSG, msg);
        b.putInt(TES_JOBPARAM_CODE, code);
        return b;
    }

    /**
     * sets data field to a Location result and returns a new bundle with the field set internally
     *
     * NOTE: we have to json things as the firebase job scheduler only supports basic data types like strings, numbers, etc..
     *
     * @param lr - location result
     * @return bunder
     */
    public static Bundle setData(LocationResult lr) {
        Bundle b = new Bundle();
        try {
            List<Location> locations = lr.getLocations();

            JSONArray locArray = TesUtils.locationsToJson(locations, true);
            JSONObject lastLoc = new TesLocationInfo(lr.getLastLocation(), true).toDictionary();
            JSONObject params = new JSONObject();
            params.put(TES_KEY_LOCATIONS, locArray);
            params.put(TES_KEY_LASTLOCATION, lastLoc);

            String json = params.toString();

            b.putInt(TES_JOBPARAM_SUCCESS, 1);
            b.putString(TES_JOBPARAM_DATA, json);
        }
        catch (JSONException e){
            setError("Faled to encode location to JSON "+e.getMessage(), -3);
        }
        return b;
    }

    /**
     * sets data field to a Geofence event result and returns a new bundle with the field set internally
     *
     * NOTE: we have to json things as the firebase job scheduler only supports basic data types like strings, numbers, etc..
     *
     * @param ge - geofence envent
     * @return bunder
     */
    public static Bundle setData(GeofencingEvent ge) {
        Bundle b = new Bundle();
        try {

            JSONObject params = new JSONObject();
            JSONArray regionIdentifiers = new JSONArray();
            for (Geofence triggered : ge.getTriggeringGeofences()) {
                regionIdentifiers.put(triggered.getRequestId());
            }
            JSONObject loc = new TesLocationInfo(ge.getTriggeringLocation(), true).toDictionary();

            params.put(TES_KEY_TRIGGERING_GEOFENCES, regionIdentifiers);
            params.put(TES_KEY_TRIGGERING_LOCATION, loc);
            params.put(TES_KEY_GEOFENCE_TRANSITION, ge.getGeofenceTransition());

            String json = params.toString();

            b.putInt(TES_JOBPARAM_SUCCESS, 1);
            b.putString(TES_JOBPARAM_DATA, json);
        }
        catch (JSONException e){
            setError("Faled to encode geoevtn to JSON "+e.getMessage(), -3);
        }
        return b;
    }

    /**
     * sets data field to a generic JSON object
     *
     * NOTE: we have to json things as the firebase job scheduler only supports basic data types like strings, numbers, etc..
     *
     * @param params JSON object
     * @return bunder
     */
    public static Bundle setData(JSONObject params) {
        Bundle b = new Bundle();

        String json = params.toString();
        b.putInt(TES_JOBPARAM_SUCCESS, 1);
        b.putString(TES_JOBPARAM_DATA, json);
        return b;
    }

    /**
     * returns previously jsonified config obect
     *
     * @param b - bundle containing jsonified config
     * @return TesConfig objecgt
     *
     */
    public static TesConfig getConfig(Bundle b) throws JSONException {
        String cfgJson = b.getString(TesJobDispatcher.TES_JOBPARAM_CONFIG);
        TesConfig cfg = new TesConfig();
        cfg.fromJSON(cfgJson);
        return cfg;
    }

    /**
     * returns a json object from bundled up data
     * @param b - bundle containting jsonified geoevent or location result
     * @return JSON obbject
     */
    public static JSONObject getJsonData(Bundle b) throws JSONException {
        String json = b.getString(TES_JOBPARAM_DATA);
        return new JSONObject(json);
    }


    /**
     * bundle check to see what success is set to
     * @param b - bunle to check
     * @return success - true
     */
    public static boolean isSuccess(Bundle b){
        return (b.getInt(TES_JOBPARAM_SUCCESS) == 1);
    }

    public static int getErrorCode(Bundle b){
        return b.getInt(TES_JOBPARAM_CODE);
    }

    public static String getErrorMessage(Bundle b){
        return b.getString(TES_JOBPARAM_MSG);
    }

    /**
     * Schedule a job
     * @param cls - class of job
     * @param delay - wait before job
     * @param deadline - how long can we wait
     * @param networkType - netword restrictions
     * @param requireIdle - require idle
     * @param requireCharging - require charging
     * @param extras - bundle to pass to the job
     */
    public void scheduleJob(@NonNull Class<? extends JobService> cls, final int delay, final int deadline, final int networkType, final boolean requireIdle, boolean requireCharging,  @Nullable Bundle extras){
        int jobId = nextJobId();
        String jobTag = String.format("JID_%d", jobId);

        Job.Builder builder =dispatcher.newJobBuilder()
                .setService(cls) // the JobService that will be called
                .setTag(jobTag)  // uniquely identifies the job
                .setRecurring(false) // one-off job
                .setLifetime(Lifetime.FOREVER)//persist forever
                .setTrigger(Trigger.executionWindow(delay, deadline))  // delay and deadline
                .setReplaceCurrent(false) // don't overwrite an existing job with the same tag
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL); //retry with exponential backoff


        int idx = 0;
        int size = 1 + ((requireIdle)?1:0) + ((requireCharging)?1:0);
        int[] constraints = new int[size];

        constraints[idx++] = networkType;
        if (requireIdle)
            constraints[idx++]=Constraint.DEVICE_IDLE;
        if (requireCharging)
            constraints[idx++]=Constraint.DEVICE_CHARGING;

        builder.setConstraints(constraints);

        if (extras==null && this.config != null) {
            extras = new Bundle();
        }

        if (this.config != null){
            try {
                extras = this.setConfig(extras);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to save config in bundle");
                e.printStackTrace();
            }
        }

        builder.setExtras(extras);

        Job myJob = builder.build();
        dispatcher.mustSchedule(myJob);
    }

    /**
     * Cancels the given job
     * @param jobId - job tag to cancel
     */
    public void cancel(final String jobId)
    {
        dispatcher.cancel(jobId);
    }

    /**
     * cancel all jobs.
     */
    public void cancelAllJobs() {
        dispatcher.cancelAll();
    }

}


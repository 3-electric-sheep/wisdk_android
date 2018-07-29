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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class TesGeofenceTransitionsIntentService extends IntentService {

    private static final String TAG = "TesGeofenceService";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public TesGeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage =String.format("Error processing geofence transition: %s", geofencingEvent.getErrorCode());
            Log.i(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
            Log.i(TAG, "Geofence trigger: ENTER fired");
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL){
            Log.i(TAG, "Geofence trigger: DWELL fired");
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, geofencingEvent);
            Log.i(TAG, geofenceTransitionDetails);

            try {
                Context context = getApplicationContext();
                TesConfig cfg  =  TesConfig.getSavedConfig(context);;
                TesJobMgr jm = new TesJobMgr(context, cfg);;

                Location location = geofencingEvent.getTriggeringLocation();
                List<Location> locationList = new ArrayList<Location>();
                locationList.add(location);

                JSONArray locArray = TesUtils.locationsToJson(locationList, true);
                JSONObject params = new JSONObject();
                params.put(TesDeviceUpdateService.PARAM_LOC_LIST, locArray);

                jm.scheduleJob(TesDeviceUpdateService.class, cfg.delay,cfg.deadline, cfg.networkType, cfg.requireIdle, cfg.requireCharging, params);

                // add new geofence at the exit point of this geofence
                jm.scheduleJob(TesGeofenceUpdateService.class,
                        TesGeofenceUpdateService.GEOFENCE_JOB_DEFAULT_DELAY,
                        TesGeofenceUpdateService.GEOFENCE_JOB_DEFAULT_DEADLINE,
                        TesGeofenceUpdateService.GEOFENCE_JOB_DEFAULT_NETWORK_TYPE,
                        TesGeofenceUpdateService.GEOFENCE_JOB_DEFAULT_REQUIRE_IDLE,
                        TesGeofenceUpdateService.GEOFENCE_JOB_DEFAULT_REQUIRE_CHARGING,
                        params);


            } catch (JSONException e) {
                Log.e(TAG, "Failed to schedule location update. invalid JSON found");
                e.printStackTrace();
            }

        } else {
            // Log the error.
            Log.e(TAG, String.format("Invalid geofence trigger: %s", geofenceTransition));
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param geofencingEvent       The geofence event that caused this trigger
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            GeofencingEvent geofencingEvent) {

        Location loc = geofencingEvent.getTriggeringLocation();
        String geofenceTransitionString = String.format("Geofence trigger: %s (%s, %s) for ", geofenceTransition, loc.getLatitude(), loc.getLongitude());

        // Get the geofences that were triggered. A single event can trigger multiple geofences.
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + " " + triggeringGeofencesIdsString;
    }

}

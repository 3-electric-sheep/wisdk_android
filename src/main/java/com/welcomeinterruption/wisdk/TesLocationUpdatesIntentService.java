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
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Handles incoming location updates and displays a notification with the location data.
 *
 * For apps targeting API level 25 ("Nougat") or lower, location updates may be requested
 * using {@link android.app.PendingIntent#getService(Context, int, Intent, int)} or
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)}. For apps targeting
 * API level O, only {@code getBroadcast} should be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
public class TesLocationUpdatesIntentService extends IntentService {

    private static final String ACTION_PROCESS_UPDATES =
            "com.welcomeinterruption.wisdk.locationupdatespendingintent.action" +
                    ".PROCESS_UPDATES";
    private static final String TAG = TesLocationUpdatesIntentService.class.getSimpleName();


    public TesLocationUpdatesIntentService() {
        // Name the worker thread.
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                Context context = this.getApplicationContext();

                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    try {
                        TesConfig cfg  =  TesConfig.getSavedConfig(context);;
                        TesJobMgr jm = new TesJobMgr(context, cfg);;

                        List<Location> locations = result.getLocations();

                        Location newestLoc = null;
                        for(Location l:locations){
                            if (newestLoc == null)
                                newestLoc = l;
                            else if (l.getTime() > newestLoc.getTime() ){
                                newestLoc = l;
                            }
                        }

                        JSONArray locArray = TesUtils.locationsToJson(locations, true);
                        JSONObject params = new JSONObject();
                        params.put(TesDeviceUpdateService.PARAM_LOC_LIST, locArray);

                        jm.scheduleJob(TesDeviceUpdateService.class, cfg.delay,cfg.deadline, cfg.networkType, cfg.requireIdle, cfg.requireCharging, params);

                        // add a geofence at the location returned by the broadcast receiver
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
                }
            }
        }
    }
}

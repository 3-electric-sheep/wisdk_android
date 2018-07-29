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

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TesGeofenceUpdateService extends JobService {
    private static final String TAG = TesGeofenceUpdateService.class.getSimpleName();

    // default job scheduler params
    public static final boolean GEOFENCE_JOB_DEFAULT_REQUIRE_CHARGING = false;
    public static final int GEOFENCE_JOB_DEFAULT_DELAY = 0;
    public static final int GEOFENCE_JOB_DEFAULT_DEADLINE = 60; /* 1 min */
    public static final int GEOFENCE_JOB_DEFAULT_NETWORK_TYPE = JobInfo.NETWORK_TYPE_NONE;
    public static final boolean GEOFENCE_JOB_DEFAULT_REQUIRE_IDLE = false ;


    public static final String PARAM_LOC_LIST = "loclist";
    private TesWIApp bgApp = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "TesGeofenceUpdateService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "TesGeofenceUpdateService destroyed");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.i(TAG, "TesGeofenceUpdateService start");
        PersistableBundle extras = params.getExtras();
        String cfgJson = extras.getString(TesJobMgr.TES_JOB_KEY_CONFIG);
        String jonParamJson = extras.getString(TesJobMgr.TES_JOB_KEY_PARAMS);

        TesConfig cfg = new TesConfig();
        JSONArray loclist;
        TesLocationInfo loc = null;
        try {
            cfg.fromJSON(cfgJson);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load config due to a JSON exception");
            e.printStackTrace();
        }

        try {
            JSONObject jobParams = new JSONObject(jonParamJson);
            loclist = jobParams.optJSONArray(PARAM_LOC_LIST);
            if (loclist != null && loclist.length()>0)
                loc = new TesLocationInfo(loclist.getJSONObject(loclist.length()-1));

        } catch (JSONException e) {
            Log.e(TAG, "Failed to load job params due to a JSON exception");
            e.printStackTrace();
        }

        if (loc != null) {
            final TesLocationInfo locFinal = loc;
            this.bgApp = TesWIApp.createManager(null, this.getApplicationContext(), -1);
            this.bgApp.startLocationMgr(cfg, new TesLocationMgr.TesLocationMgrListener() {
                @Override
                public void sendDeviceUpdate(@NonNull TesLocationInfo locInfo, boolean inBackground, TesApi.TesApiListener listener) throws JSONException {
                    Log.e(TAG, "Geofence update - sendDeviceUpdate callback called - something is wrong");
                }

                @Override
                public void onConnected(@Nullable Bundle bundle) {
                    Log.i(TAG, "Geofence update - connected - adding new geofence");
                    bgApp.removeGeofence();
                    bgApp.addGeofence(locFinal.latitude, locFinal.longitude);
                    jobFinished(params, false);
                }

                @Override
                public void onConnectionSuspended(int i) {
                    Log.i(TAG, "Geofence update - connection suspended - can't set new geofence");
                    jobFinished(params, false);
                }

                @Override
                public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                    Log.e(TAG, "Geofence update - connection failed - can't set new geofence");
                    jobFinished(params, false);
                }

                @Override
                public void onError(@Nullable String msg) {
                    Log.e(TAG, String.format("Geofence update - connection error - can't set new geofence: %s", msg));
                    jobFinished(params, false);
                }
            });

            // Return true as there's more work to be done with this job.
            return true;
        }

        // nothing to do
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {

        /*
        if (this.task != null)
            this.task.cancel(true);
        */

        return false;
    }

}

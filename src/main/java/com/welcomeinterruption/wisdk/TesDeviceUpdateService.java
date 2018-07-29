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

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TesDeviceUpdateService extends JobService {
    private static final String TAG = TesDeviceUpdateService.class.getSimpleName();

    public static final String PARAM_LOC_LIST = "loclist";
    //private SendDeviceUpdateTask task = null;
    private TesWIApp bgApp = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "TesDeviceUpdateService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "TesDeviceUpdateService destroyed");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.i(TAG, "TesDeviceUpdateService start");
        PersistableBundle extras = params.getExtras();
        String cfgJson = extras.getString(TesJobMgr.TES_JOB_KEY_CONFIG);
        String jonParamJson = extras.getString(TesJobMgr.TES_JOB_KEY_PARAMS);

        TesConfig cfg = new TesConfig();
        JSONArray loclist = null;
        try {
            cfg.fromJSON(cfgJson);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load config due to a JSON exception");
            e.printStackTrace();
        }

        try {
            JSONObject jobParams = new JSONObject(jonParamJson);
            loclist = jobParams.optJSONArray(PARAM_LOC_LIST);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load job params due to a JSON exception");
            e.printStackTrace();
        }
       if (loclist != null && loclist.length()>0) {
            this.bgApp = TesWIApp.createManager(null, this.getApplicationContext(), -1);
            this.bgApp.startApi(cfg);

            for (int i=0; i<loclist.length(); i++) {
                try {
                    TesLocationInfo loc = new TesLocationInfo(loclist.getJSONObject(i));
                    this.bgApp.sendDeviceUpdate(loc, true, new TesApi.TesApiListener() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            jobFinished(params, false);
                        }

                        @Override
                        public void onFailed(JSONObject result) {
                            jobFinished(params, false); // no rescheudle as its a data error rather than connectivity
                        }

                        @Override
                        public void onOtherError(TesApiException error) {
                            jobFinished(params, false); // no rescedule as this was a non specified error

                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to process device update due to JSON error");
                    e.printStackTrace();
                }
            }

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

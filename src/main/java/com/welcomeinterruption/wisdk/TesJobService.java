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

import android.content.Intent;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.JobParameters;

import java.util.Map;

public abstract class TesJobService extends JobService {
    private static final String TAG = TesJobService.class.getSimpleName();

    public abstract boolean doWork(final JobParameters params, JSONObject jsArgs);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "--> Job Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "--> Job Service destroyed");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.i(TAG, "--> Job Service started");
        if (params.getExtras() == null) {
            return true; // nothing to do
        }

        Bundle extras = params.getExtras();

        TesConfig cfg = null;
        try {
            cfg = TesJobDispatcher.getConfig(extras);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load config due to a JSON exception. Creating default config");
            e.printStackTrace();
            cfg = new TesConfig();
        }

        JSONObject jsArgs = new JSONObject();

        try {
            if (!TesJobDispatcher.isSuccess(extras)) {
                jsArgs.put("success", false);
                jsArgs.put("code", TesJobDispatcher.getErrorCode(extras));
                jsArgs.put("error", TesJobDispatcher.getErrorMessage(extras));
            }
            else {
                JSONObject result = null;
                result = TesJobDispatcher.getJsonData(extras);
                jsArgs.put("success", true);
                jsArgs.put("data", result);
            }
        } catch (JSONException e) {
            try {
                jsArgs.put("success", false);
                jsArgs.put("code", TesJobDispatcher.ERROR_JSON_ENCODE_DECODE);
                jsArgs.put("error", "Failed to decode JSON geofence event results: " + e.getMessage());
            }
            catch (JSONException a) {
                Log.e(TAG, "JSON exception within exception: "+a.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        // kick the system
        this.startWiApp(cfg, false);

        // return true if there is more to do else false
        return this.doWork(params, jsArgs);
    }

    public void startWiApp(TesConfig cfg, boolean fullStarup){
        TesWIApp wi = TesWIApp.manager();
        if (wi == null) {
            wi = TesWIApp.createManager(this, new TesWIApp.TesWIAppListener() {
                @Override
                public void onStartupComplete(boolean isAuthorized) {
                    Log.i(TAG, "BG OnStartupComplete");

                }

                @Override
                public boolean onLocationPermissionCheck(String result, boolean just_blocked) {
                    if (result == "restricted" && !just_blocked)
                        return true;
                    return false;
                }

                @Override
                public void onLocationUpdate(TesLocationInfo loc) {
                    Log.i(TAG, String.format("--> BG onLocationUpdate: %s", loc.toString()));
                }

                @Override
                public void onGeoLocationUpdate(TesLocationInfo loc) {
                    Log.i(TAG, String.format("--> BG onGeoLocationUpdate: %s", loc.toString()));
                }

                @Override
                public void authorizeFailure(int statusCode, byte[] data, boolean notModified, long networkTimeMs, Map<String, String> headers) {
                    Log.i(TAG, String.format("--> BG authorizeFailure: %d", statusCode));
                }

                @Override
                public void onAutoAuthenticate(int status, @Nullable JSONObject responseObject, @Nullable Exception error) {
                    Log.i(TAG, String.format("--> DB onAutoAuthenticate: %d %s", status, responseObject.toString()));
                }

                @Override
                public void newAccessToken(@Nullable String token) {
                    Log.i(TAG, String.format("--> BG newAccessToken: %s", token));

                }

                @Override
                public void newDeviceToken(@Nullable String token) {
                    Log.i(TAG, String.format("--> BG newDeviceToken: %s", token));
                }

                @Override
                public void newPushToken(@Nullable String token) {
                    Log.i(TAG, String.format("--> BG newPushToken: %s", token));
                }

                @Override
                public void onRemoteNotification(@Nullable JSONObject data) {
                    Log.i(TAG, String.format("--> BG onRemoteNotification: %s", data.toString()));
                }

                @Override
                public void onRemoteDataNotification(@Nullable JSONObject data) {
                    Log.i(TAG, String.format("--> BG onRemoteDataNotification: %s", data.toString()));
                }

                @Override
                public void onWalletNotification(@Nullable JSONObject data) {
                    Log.i(TAG, String.format("--> BG onWalletNotification: %s", data.toString()));
                }

                @Override
                public void onRefreshToken(@NonNull String token) {
                    Log.i(TAG, String.format("--> BG onRefreshToken: %s", token));
                }

                @Override
                public void saveWallet(int requestCode, int resultCode, Intent data, String msg) {
                    Log.i(TAG, String.format("--> BG saveWallet: %d %d %s Intent: %s", requestCode, resultCode, msg, data.toString()));
                }

                @Override
                public void onError(String errorType, Exception error) {
                    Log.i(TAG, String.format("--> BF onError: %s %s", errorType, error.getMessage()));
                }
            });
        }

        if (fullStarup){
            wi.start(cfg);
        }
        else {
            wi.startApi(cfg);
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

}

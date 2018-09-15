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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import org.json.JSONException;

/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should not be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
public class TesLocationUpdatesBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "LUBroadcastReceiver";

    static final String ACTION_PROCESS_UPDATES =
            "com.welcomeinterruption.wisdk.locationupdatespendingintent.action.PROCESS_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null){
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                TesConfig cfg = new TesConfig();
                try {
                    cfg = TesConfig.getSavedConfig(context);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to get saved configs - using defaults. invalid JSON found");
                    e.printStackTrace();
                }

                LocationResult result = LocationResult.extractResult(intent);

                Bundle b;
                if (result == null) {
                    b = TesJobDispatcher.setError("No location received", -1);
                } else {
                    b = TesJobDispatcher.setData(result);
                }

                TesJobDispatcher jm = new TesJobDispatcher(context, cfg);
                jm.scheduleJob(TesLocationUpdateService.class,
                        cfg.delay,
                        cfg.deadline,
                        cfg.networkType,
                        cfg.requireIdle,
                        cfg.requireCharging,
                        b);
            }

        }
    }
}

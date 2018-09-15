package com.welcomeinterruption.wisdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;

/**
 * On Android geofences are cleared after a device restart, so we tell the sdk we have been rebooted
 */
public class TesBootBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG = "RNWiBootReceiver";
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.e(TAG, "In boot receiver");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            TesConfig cfg = new TesConfig();
            try {
                cfg = TesConfig.getSavedConfig(context);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get saved configs - using defaults. invalid JSON found");
                e.printStackTrace();
            }

            TesJobDispatcher jm = new TesJobDispatcher(context, cfg);
            jm.scheduleJob(TesBootService.class,
                    cfg.delay,
                    cfg.deadline,
                    cfg.networkType,
                    cfg.requireIdle,
                    cfg.requireCharging, null);
        }

    }
}
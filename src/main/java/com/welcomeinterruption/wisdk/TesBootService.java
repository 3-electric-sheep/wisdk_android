package com.welcomeinterruption.wisdk;
import android.util.Log;
import com.firebase.jobdispatcher.JobParameters;
import org.json.JSONObject;

public class TesBootService extends TesJobService
{
    private static final String TAG = "TesBootService";


    @Override
    public boolean doWork(JobParameters params, JSONObject jsArgs) {
        Log.d(TAG, "Boot receiver fired");
        // TODO: fill this out
        return false;
    }
}
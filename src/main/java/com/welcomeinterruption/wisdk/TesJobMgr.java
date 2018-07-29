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
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class TesJobMgr {
    public static String TES_JOB_KEY_CONFIG = "TesJobKeyConfig";
    public static String TES_JOB_KEY_PARAMS = "TesJobKeyParamns";

    private static final String TAG = "TesJobMgr";

    private @NonNull Context mCtx;
    private @Nullable TesConfig mConfig = null;

    private static int mJobId = 0;

    public static synchronized int nextJobId() {
        return ++mJobId;
    }

    public static synchronized int getJobId() {
        return mJobId;
    }

    public TesJobMgr(@NonNull Context ctx, final @Nullable TesConfig cfg){
        this.mCtx = ctx;
        this.mConfig = cfg;
    }

    /**
     * Schedule a job with the particular service component
     * @param delay min time before job is started
     * @param deadline max time before job is started
     * @param networkType type of network needed
     * @param requireIdle need to be idle
     * @param requireCharging need to be charging
     * @param params json params to job
     * @return
     */
    public int scheduleJob(@NonNull Class<?> cls, final int delay, final int deadline, final int networkType, final boolean requireIdle, boolean requireCharging, final @Nullable JSONObject params) throws JSONException {
        ComponentName mServiceComponent = new ComponentName(this.mCtx, cls);

        int jobId = nextJobId();
        JobInfo.Builder builder = new JobInfo.Builder(jobId, mServiceComponent);
        if (delay > 0) {
            builder.setMinimumLatency(delay * 1000);
        }

        if (deadline > 0) {
            builder.setOverrideDeadline(deadline * 1000);
        }

        builder.setRequiredNetworkType(networkType);
        builder.setRequiresDeviceIdle(requireIdle);
        builder.setRequiresCharging(requireCharging);

        PersistableBundle extras = new PersistableBundle();
        boolean haveExtra = false;
        if (this.mConfig != null){
            extras.putString(TES_JOB_KEY_CONFIG, this.mConfig.toJSON().toString());
            haveExtra = true;
        }
        if (params != null){
            extras.putString(TES_JOB_KEY_PARAMS, params.toString());
            haveExtra = true;
        }

        if (haveExtra) {
            builder.setExtras(extras);
        }

        // Schedule job
        Log.d(TAG, "Scheduling job");
        JobScheduler tm = (JobScheduler) mCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.schedule(builder.build());
        return jobId;
    }

    /**
     * Cancels the given job
     * @param jobId
     */
    public void cancel(final int jobId)
    {
        JobScheduler jobScheduler = (JobScheduler) mCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(jobId);
    }

    /**
     * Executed when user clicks on CANCEL ALL.
     */
    public void cancelAllJobs() {
        JobScheduler tm = (JobScheduler) mCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.cancelAll();
    }

    /**
     * Executed when user clicks on FINISH LAST TASK.
     */
    public void finishLastJob() {
        JobScheduler jobScheduler = (JobScheduler) mCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = jobScheduler.getAllPendingJobs();
        if (allPendingJobs.size() > 0) {
            // Finish the last one
            int jobId = allPendingJobs.get(0).getId();
            jobScheduler.cancel(jobId);
        } else {
            Log.i(TAG, "No pending jobs to cancel");
       }
    }
}

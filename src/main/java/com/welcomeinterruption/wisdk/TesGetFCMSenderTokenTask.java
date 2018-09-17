package com.welcomeinterruption.wisdk;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

/**
 * Created by pfrantz on 17/9/18.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */
public class TesGetFCMSenderTokenTask extends AsyncTask<String, Void, String> {

    private static final String TAG = "TesFCMSenderTokenTask";

    private Context wiCtx;
    private TesPushMgr pushMgr;
    private Exception taskException;
    private String senderId;

    TesGetFCMSenderTokenTask(Context ctx, TesPushMgr pushMgr){
        this.pushMgr = pushMgr;
        this.taskException = null;
        this.senderId = null;
        this.wiCtx = ctx;
    }

    @Override
    protected String doInBackground(String... strings)  {
        String token=null;
        this.senderId  = strings[0];
        try {
            token = FirebaseInstanceId.getInstance().getToken(this.senderId , FirebaseMessaging.INSTANCE_ID_SCOPE);
        } catch (IOException e) {
            Log.e(TAG, "Failed to get notification push token for senderid " + this.senderId  + ": " + e.getLocalizedMessage());
            this.taskException = e;
        }
        return token;
    }

    @Override
    protected void onPostExecute(String token) {
        super.onPostExecute(token);

        if (this.taskException != null){
            if (this.pushMgr.listener != null)
                this.pushMgr.listener.onPushMgrError(this.taskException);
        }
        else {
            this.pushMgr.fcmToken = token;
            Log.d(TAG, String.format("FCM For sender id %s token: %s" , this.senderId, this.pushMgr.fcmToken));

            final Intent intent = new Intent(TesPushMgr.NOTIFICATION_PUSH_RECEIVER);

            final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this.wiCtx);
            intent.putExtra(TesPushMgr.NOTIFICATION_TYPE_KEY, TesPushMgr.NOTIFICATION_TYPE_REFRESH);
            intent.putExtra(TesPushMgr.NOTIFICATION_TOKEN_KEY,this.pushMgr.fcmToken );
            broadcastManager.sendBroadcast(intent);
        }
    }

}

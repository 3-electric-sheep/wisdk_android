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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TesPushMgr {
    public final static String NOTIFICATION_CHANNEL_ID = "wi_default_channel_001";
    public final static String NOTIFICATION_CHANNEL_NAME = "wi_offers";
    public final static String NOTIFICATION_TOPIC_OFFERS = "wi_offers";

    public final static String NOTIFICATION_PUSH_RECEIVER = "wi_token_receiver";

    public final static String NOTIFICATION_TYPE_KEY = "type";
    public final static String NOTIFICATION_TOKEN_KEY = "token";
    public final static String NOTIFICATION_DATA_KEY = "data";
    public final static String NOTIFICATION_NOTIFY_KEY = "notify";

    public final static String NOTIFICATION_TYPE_REFRESH = "refresh";
    public final static String NOTIFICATION_TYPE_DATA_MSG = "data";
    public final static String NOTIFICATION_TYPE_NOTIFY = "notify";

    private static final String TAG = "TESPushMgr";

    public interface TesPushMgrListener
    {
        public void onRefreshToken(String token);
        public void onDataMsg(JSONObject data);
        public void onNotificationMsg(JSONObject data);
        public void onPushMgrError(Exception e);
    };

    /**
     * Config for the system
     */
    private final TesConfig config;


    /**
     * Context to the main activity that all the intents will be bound too
     */
    private Context mCtx;
    public String pushProfile;
    public String fcmToken;
    public BroadcastReceiver tokenReceiver;

    public TesPushMgrListener listener;

    TesPushMgr(@Nullable String pushProfile,  Context ctx, TesConfig config) {
        this.config = config;
        this.mCtx = ctx;
        this.pushProfile = pushProfile;
        this.fcmToken = null;
        this.listener = null;
    }

    public void registerRemoteNotifications(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = NOTIFICATION_CHANNEL_ID;
            String channelName = NOTIFICATION_CHANNEL_NAME;
            NotificationManager notificationManager = this.mCtx.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW));
        }

        this.tokenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String type = intent.getStringExtra(NOTIFICATION_TYPE_KEY);
                if (type.equals(NOTIFICATION_TYPE_REFRESH)) {
                    String token = intent.getStringExtra(NOTIFICATION_TOKEN_KEY);
                    if (token != null) {
                        //send token to your server or what you want to do
                        if (listener != null)
                            listener.onRefreshToken(token);
                    }
                }
                else if (type.equals(NOTIFICATION_TYPE_DATA_MSG)){
                    String data = intent.getStringExtra(NOTIFICATION_DATA_KEY);
                    if (data != null) {
                        // send data to you listener
                        try {
                            JSONObject json = new JSONObject(data);
                            if (listener != null)
                                listener.onDataMsg(json);
                        } catch (JSONException e) {
                            Log.e(TAG, "Invalid notification data packet: "+data);
                            e.printStackTrace();
                        }
                    }
                }
                else if (type.equals(NOTIFICATION_TYPE_NOTIFY)){
                    String data = intent.getStringExtra(NOTIFICATION_DATA_KEY);
                    if (data != null) {
                        // send data to you listener
                        try {
                            JSONObject json = new JSONObject(data);
                            if (listener != null)
                                listener.onNotificationMsg(json);
                        } catch (JSONException e) {
                            Log.e(TAG, "Invalid notification message packet: "+data);
                            e.printStackTrace();
                        }
                    }
                }

            }
        };

        LocalBroadcastManager.getInstance(this.mCtx).registerReceiver(tokenReceiver,
                new IntentFilter(NOTIFICATION_PUSH_RECEIVER));

        final String senderId = this.config.fcmSenderId;
        if (senderId != null && senderId.length()> 0){
            // get token via sender id
            TesGetFCMSenderTokenTask task = new TesGetFCMSenderTokenTask(this.mCtx, this);
            task.execute(senderId);
        }
        else {
            // Get default token for app
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "getInstanceId failed", task.getException());
                                if (TesPushMgr.this.listener != null) {
                                    TesPushMgr.this.listener.onPushMgrError(task.getException());
                                }
                                return;
                            }
                            // Get new Instance ID token
                            TesPushMgr.this.fcmToken = task.getResult().getToken();
                            Log.d(TAG, "FCM token:" + TesPushMgr.this.fcmToken );

                            final Intent intent = new Intent(TesPushMgr.NOTIFICATION_PUSH_RECEIVER);

                            final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(TesPushMgr.this.mCtx);
                            intent.putExtra(TesPushMgr.NOTIFICATION_TYPE_KEY, TesPushMgr.NOTIFICATION_TYPE_REFRESH);
                            intent.putExtra(TesPushMgr.NOTIFICATION_TOKEN_KEY,TesPushMgr.this.fcmToken );
                            broadcastManager.sendBroadcast(intent);
                        }
                    });
        }

        FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_OFFERS);
    }

    public String getToken()
    {
        return fcmToken;
    }

}

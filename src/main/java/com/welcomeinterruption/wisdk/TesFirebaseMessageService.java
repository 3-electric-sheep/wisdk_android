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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class TesFirebaseMessageService extends FirebaseMessagingService {

    private static final String TAG = "TesFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            // SHOULD EMULATE data key here and bundle notification as mesage.

            RemoteMessage.Notification notification = remoteMessage.getNotification();
            Log.d(TAG, "Message Notification Body: " + notification.getBody());

            JSONObject json = new JSONObject();
            try {
                json.put("title", notification.getTitle() );
                json.put("body", notification.getBody());
            } catch (JSONException e) {
                e.printStackTrace();
            }


            final Intent intent = new Intent(TesPushMgr.NOTIFICATION_PUSH_RECEIVER);
            // You can also include some extra data.
            final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            intent.putExtra(TesPushMgr.NOTIFICATION_TYPE_KEY, TesPushMgr.NOTIFICATION_TYPE_NOTIFY);
            intent.putExtra(TesPushMgr.NOTIFICATION_DATA_KEY,json.toString());
            broadcastManager.sendBroadcast(intent);
       }

        // Check if message contains a data payload which means its a GCM message.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            JSONObject json = new JSONObject(remoteMessage.getData());

            final Intent intent = new Intent(TesPushMgr.NOTIFICATION_PUSH_RECEIVER);
            // You can also include some extra data.
            final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            intent.putExtra(TesPushMgr.NOTIFICATION_TYPE_KEY, TesPushMgr.NOTIFICATION_TYPE_DATA_MSG);
            intent.putExtra(TesPushMgr.NOTIFICATION_DATA_KEY,json.toString());
            broadcastManager.sendBroadcast(intent);

        }


        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.d(TAG, "Refreshed token: " + token);

        final Intent intent = new Intent(TesPushMgr.NOTIFICATION_PUSH_RECEIVER);

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        intent.putExtra(TesPushMgr.NOTIFICATION_TYPE_KEY, TesPushMgr.NOTIFICATION_TYPE_REFRESH);
        intent.putExtra(TesPushMgr.NOTIFICATION_TOKEN_KEY,token);
        broadcastManager.sendBroadcast(intent);
    }
    // [END receive_message]


}

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


import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility methods used in this sample.
 */
class TesUtils {

    final static String KEY_LOCATION_UPDATES_REQUESTED = "location-updates-requested";
    final static String KEY_LOCATION_UPDATES_RESULT = "location-update-result";

    // TODO: add keys for config here and save/restore through shared prefs, this context
    // can then be used wherever needed to get current config details/

    private static final String TAG = "TESUtils";

    static void setRequestingLocationUpdates(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_LOCATION_UPDATES_REQUESTED, value)
                .apply();
    }

    static boolean getRequestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_LOCATION_UPDATES_REQUESTED, false);
    }

    /**
     * Returns the title for reporting about a list of {@link Location} objects.
     *
     * @param context The {@link Context}.
     */
    static String getLocationResultTitle(Context context, List<Location> locations) {
        String numLocationsReported = context.getResources().getQuantityString(
                R.plurals.num_locations_reported, locations.size(), locations.size());
        return numLocationsReported + ": " + DateFormat.getDateTimeInstance().format(new Date());
    }

    /**
     * Returns te text for reporting about a list of  {@link Location} objects.
     *
     * @param locations List of {@link Location}s.
     */
    private static String getLocationResultText(Context context, List<Location> locations) {
        if (locations.isEmpty()) {
            return context.getString(R.string.unknown_location);
        }
        StringBuilder sb = new StringBuilder();
        for (Location location : locations) {
            sb.append("(");
            sb.append(location.getLatitude());
            sb.append(", ");
            sb.append(location.getLongitude());
            sb.append(")");
            sb.append("\n");
        }
        return sb.toString();
    }

    static void setLocationUpdatesResult(Context context, List<Location> locations) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_LOCATION_UPDATES_RESULT, getLocationResultTitle(context, locations)
                        + "\n" + getLocationResultText(context, locations))
                .apply();
    }

    static String getLocationUpdatesResult(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LOCATION_UPDATES_RESULT, "");
    }

    /**
     * -------------------
     * date util functions
     * -------------------
     **/

    static public Date dateFromString(@Nullable String dateString) throws ParseException {
        if (dateString == null || dateString.length() < 1)
            return null;

        return TesISO8601DateParser.parse(dateString);
    }

    static public String stringFromDate(@Nullable Date date)
    {
        if (date == null)
            return null;

        return TesISO8601DateParser.toString(date);
    }

    /**
     * JSON helpers
     */

    public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) throws JSONException {
        final JSONObject mergedJSON = new JSONObject();

        Iterator<String> i = json1.keys();
        while (i.hasNext()){
            String key = i.next();
            mergedJSON.put(key, json1.get(key));
        }

        i = json2.keys();
        while (i.hasNext()){
            String key = i.next();
            mergedJSON.put(key, json2.get(key));
        }

        return mergedJSON;
    }

    /**
     * Finds the index of the object who key has the given value
     * @param key the key of the object entry
     * @param value the value of the key to look for
     * @param stringArray the jsonarray to search
     * @return -1 = not found else the index
     */
    public static int findJsonArrayIndex(String key, String value, JSONArray stringArray){
        int index = -1;
        for (int i=0; i<stringArray.length(); i++){
            JSONObject obj = null;
            try {
                obj = stringArray.getJSONObject(i);
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get list: %s", e.toString()));
            }

            String oid = null;
            try {
                oid = obj.getString(key);
                if (oid.equals(value)){
                    index = i;
                    break;
                }
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get key list entry: %s", e.toString()));

            }
        }
        return index;
    }

    public static int findJsonArrayIndex(String value, JSONArray stringArray){
        int index = -1;
        for (int i=0; i<stringArray.length(); i++){
            String obj = null;
            try {
                obj = stringArray.getString(i);
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get list: %s", e.toString()));
            }

            if (obj != null && obj.equals(value)){
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Converts a list of locations to a JSON array
     * @param locations location list
     * @param inBackground whether the list came while the app was not running in foreground.
     * @return location list as a JSON array
     * @throws JSONException
     */
    public static JSONArray locationsToJson(List<Location> locations, boolean inBackground) throws JSONException
    {
        JSONArray json = new JSONArray();
        for (Location loc: locations){
            json.put(new TesLocationInfo(loc, inBackground).toDictionary());
        }
        return json;
    }

    static public  boolean isAppOnForeground(Context context) {
        /**
         We need to check if app is in foreground otherwise the app will crash.
         http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
         **/
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;

            }
        }
        return false;
    }


    /**
     * Notification support
     */

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
   public static void sendNotification(Context context,  Class<?> cls, String title, String messageBody ,Map<String, String> options) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId =TesPushMgr.NOTIFICATION_CHANNEL_ID;
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        //.setSmallIcon(R.drawable.ic_stat_ic_notification)
                        //.setSmallIcon(R.mipmap.ic_launcher)
                        //.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }



    public static String uuid(){
       return UUID.randomUUID().toString();
    }



}

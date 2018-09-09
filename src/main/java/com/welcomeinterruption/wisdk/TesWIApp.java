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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;


import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.common.ConnectionResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class TesWIApp implements TesLocationMgr.TesLocationMgrListener,
        TesApi.TesApiAuthListener,
        TesPushMgr.TesPushMgrListener {
    /**
     * User settings
     */
    private final static String USER_SETTINGS_PREF = "WIUserSettings";

    /**
     * API constants
     */

    private final static String TES_PATH_REGISTER = "account";
    private final static String TES_PATH_LOGIN = "auth/login";
    private final static String TES_PATH_PROFILE = "account";

    private final static String TES_PATH_GEODEVICE = "geodevice";

    private final static String TES_PATH_LIVE_EVENTS = "geodevice/%s/live-events";
    private final static String TES_PATH_LIVE_EVENTS_REVIEWS = "live-events/%s/reviews";
    private final static String TES_PATH_LIVE_EVENTS_CREATE_REVIEW = "live-events/%s/reviews";
    private final static String TES_PATH_EVENTS_REVIEWS = "events/%s/reviews";

    private final static String TES_PATH_SEARCH_LIVE_EVENTS = "geopos/%s,%s/live-events";

    private final static String TES_PATH_PROVIDERS = "providers";
    private final static String TES_PATH_PROVIDER = "providers/%s";

    private final static String TES_PATH_PROVIDER_CREATE_EVENT = "providers/%s/events";
    private final static String TES_PATH_POI_CREATE_EVENT = "poi/%s/events";

    private final static String TES_PATH_PROVIDER_LIVE_EVENTS = "providers/%s/live-events";
    private final static String TES_PATH_PROVIDER_EVENTS = "providers/%s/events";

    private final static String TES_PATH_LIVE_EVENTS_RUD = "live-events/%s";
    private final static String TES_PATH_EVENTS_RUD = "events/%s";
    private final static String TES_PATH_LIVE_EVENTS_ACK = "live-events/%s/ack";
    private final static String TES_PATH_LIST_LIVE_EVENTS_ACK = "live-events/acknowledged";
    private final static String TES_PATH_LIST_LIVE_EVENTS_FOLLOW = "live-events/following";

    private final static String TES_PATH_LIVE_EVENTS_ENACTED = "live-events/%s/enact";
    private final static String TES_PATH_LIVE_EVENTS_SHARE = "live-events/%s/share";

    private final static String TES_PATH_POI = "providers/%s/poi";
    private final static String TES_PATH_PLACE_RUD = "poi/%s";

    private final static String TES_PATH_PLACE_REVIEWS = "poi/%s/reviews";
    private final static String TES_PATH_PLACE_CREATE_REVIEW = "poi/%s/reviews";

    private final static String TES_PATH_PROVIDER_RECEIPT = "providers/%s/receipts";

    private final static String TES_PATH_REVIEW_RUD = "reviews/%s";

    private final static String TES_PATH_PLACE_IMAGES = "poi/%s/images";
    private final static String TES_PATH_ACCOUNT_PROFILE_IMAGES = "account/current/images";
    private final static String TES_PATH_IMAGES_RUD = "/images/%s.%s";
    private final static String TES_PATH_ANY_IMAGES_RUD = "/images/%s";

    private final static String TES_PATH_GEOPOS_GEODEVICES = "geopos/%f,%f/geodevices";

    private final static String TES_PATH_PRODUCT_LIST = "products/itunes";

    private final static String TES_PATH_ADDRESS_LOOKUP = "providers/address";

    /**
     * Error codes returned by the server
     **/
    public final int ERROR_NOT_FOUND = 1;
    public final int ERROR_ADD = 2;
    public final int ERROR_UPDATE = 4;

    public final String ACCESS_TOKEN_KEY = "accessToken";
    public final String ACCESS_AUTH_TYPE = "accessAuthType";
    public final String ACCESS_USER_NAME = "accessUserName";

    public final String ACCESS_SYSTEM_DEFAULTS = "accessSystemDefaults";
    public final String ACCESS_USER_SETTINGS = "accessUserSettings";

    public final String DEVICE_TOKEN_KEY = "deviceToken";
    public final String PUSH_TOKEN_KEY = "pushToken";

    public final String LOCALE_TOKEN_KEY = "localeToken";
    public final String TIMEZONE_TOKEN_KEY = "localeTimezoneToken";

    public final String LAST_LAUNCHED_VERSION_TOKEN_KEY = "lastLaunchedVersion"; // version | devel/staging/prod

    public final String SEARCH_DISTANCE_KEY = "searchDistance"; // numeric
    public final String SEARCH_CURRENT_LOC_KEY = "searchCurrentLocation";  // YES:NO
    public final String SEARCH_LOCATION_LAT_KEY = "searchLocationLat"; // latitude
    public final String SEARCH_LOCATION_LONG_KEY = "searchLocationLng"; // longitude
    public final String SEARCH_LOCATION_TEXT_KEY = "searchLocationText"; // location description

    public final String ALL_SELLER_PLACES = "ALL";
    public final String EXCLUDE_ALL_CATEGORIES = "*";

    private static final String TAG = "TesWIApp";

    /**
     * Listener interface used to tell the host of interesting things that may happen.
     */
    public interface TesWIAppListener {

        /**
         * sent when authorization has failed (401)
         *
         * @param statusCode the HTTP status code
         * @param data Response body
         * @param notModified True if the server returned a 304 and the data was already in cache
         * @param networkTimeMs Round-trip network time to receive network response
         * @param headers map of headers returned with this response, or null for none
         **/
        void authorizeFailure(int statusCode,  byte[] data,  boolean notModified, long networkTimeMs, Map<String, String> headers);

        /**
         * sent when authorization is complete
         *
         * @param status         TESCallStatus value
         * @param responseObject JSONObject response object from call
         * @param error          TesApiException set on error or nill
         **/
        void onAutoAuthenticate(int status, @Nullable JSONObject responseObject, @Nullable TesApiException error);

        /**
         * sent when a new access token is returned
         */
        void newAccessToken(@Nullable String token);

        /**
         * sent when a new device token has been created
         */
        void newDeviceToken(@Nullable String token);

        /**
         * sent when a new push token is returned
         */
        void newPushToken(@Nullable String token);

        /**
         * Called when a remmote notification needs to be processed
         */
        void onRemoteNotification(@Nullable JSONObject data);


        /**
         * Called when a remmote notification needs to be processed
         */
        void onRemoteDataNotification(@Nullable JSONObject data);


        /**
         * Called when a remmote notification needs to be processed
         */
        void onWalletNotification(@Nullable JSONObject data);

        /**
         * Called when remote notifications is registered or fails to register
         *
         * @param token the new token
         */
        void onRefreshToken(@NonNull String token);

        /**
         * Called when a wallet object is saved to the wallet
         *
         * @param  requestCode the code of the request
         * @param  resultCode the result code.
         * @param  data extra stuff with the save to wallet
         * @param  msg description of return code
         *
         */
        void saveWallet(int requestCode, int resultCode, Intent data, String msg);
    }

    //----------------------------------------------
    // TESWIApp interface
    //----------------------------------------------

    /**
     * the singleton wiApp object
     */
    private static TesWIApp wiInstance;
    private static boolean wiInitDone;

    static {
        wiInstance = null;
        wiInitDone = false;
    }

    /**
     * Activity that the class is bound too
     */
    private FragmentActivity wiActivity;
    /**
     * context for class
     */
    private Context wiCtx;

    /**
     * view id to anchor snackbars too
     */
    private int mViewId;

    /**
     * class level listener
     */
    public @Nullable
    TesWIAppListener listener;

    /**
     * environment details
     * <p>
     * used to specify:-
     * <p>
     * server
     * testserver
     * push profile
     * test push profile
     * cache size
     */
    public @NonNull
    TesConfig config;

    /**
     * API createManager for dealing with all REST based api calls
     **/
    public @NonNull
    TesApi api;

    /**
     * Location createManager for dealing with location monitoring
     **/
    public @Nullable
    TesLocationMgr locMgr;

    /**
     * Push createManager for dealing with push notification
     **/
    public @Nullable
    TesPushMgr pushMgr;

    /**
     * Wallet createManager for dealing with wallet based events
     **/
    public @Nullable
    TesWalletMgr walletMgr;

    /**
     * username associated with current api access token
     */

    public @Nullable
    String authUserName;

    /**
     * device Token associated with this device
     **/
    public @Nullable
    String deviceToken;

    /**
     * APN push token associated with this device
     **/
    public @Nullable
    String pushToken;

    /**
     * provider token - all users and secure items will be tied to this provider
     */

    public @Nullable
    String providerToken;

    /**
     * currently saved locale info
     */

    public @Nullable
    String localeToken;

    /**
     * currently saved token info
     */

    public @Nullable
    String timezoneToken;

    /**
     * currently saved last run version info
     **/
    public @Nullable
    String versionToken;


    /**
     * set while registering or login
     */
    public boolean _isAuthenticating;

    public synchronized boolean isAuthenticating() {
        return _isAuthenticating;
    }

    public synchronized void setAuthenticating(boolean _isAuthenticating) {
        this._isAuthenticating = _isAuthenticating;
    }

    /**
     * shared settings object
     */
    private SharedPreferences sharedPrefs = null;

    /* internal flags */
    private boolean newPushToken;
    private boolean newDeviceToken;
    private boolean newAccessToken;
    private boolean newAccessAuthType;
    private boolean newAccessUserName;
    private boolean newAccessSystemDefaults;
    private boolean newLocaleToken;
    private boolean newTimezoneToken;
    private boolean newVersionToken;

    /**
     * Creates and returns an `TESWIApp` object.
     */
    public TesWIApp(FragmentActivity fragment, Context context, int viewId) {
        this.wiActivity = fragment;
        this.wiCtx = context;
        this.mViewId = viewId;

        this.listener = null;
        this.config = null;
        this.api = null;
        this.locMgr = null;
        this.pushMgr = null;
        this.walletMgr = null;
        this.authUserName = null;
        this.deviceToken = null;
        this.pushToken = null;
        this.providerToken = null;
        this.localeToken = null;
        this.timezoneToken = null;
        this.versionToken = null;
        this.setAuthenticating(false);

        this.sharedPrefs = this.wiCtx.getSharedPreferences(this.USER_SETTINGS_PREF, Context.MODE_PRIVATE);

        this.setLocaleAndVersionInfo();
    }

    //----------------------------------------------
    // Core methods
    //--------------------------------------------

    /**
     * shared client is a singleton used to make all store related callses
     */

    public static synchronized TesWIApp createManager(FragmentActivity fragment, Context context, int viewId) {
        if (wiInstance == null) {
            wiInstance = new TesWIApp(fragment, context, viewId);
            wiInitDone = false;
        }
        return wiInstance;
    }

    public static synchronized TesWIApp manager() {
        return wiInstance;
    }

    public static synchronized void setManager(TesWIApp app) {
        wiInstance = app;
    }

    public static synchronized void setInitDone() {
        wiInitDone = true;
    }

    public static synchronized boolean hasInit() {
        return wiInitDone;
    }

    /**
     * Start the framework. This initialises location services, boots up the push createManager and authenticates with the wi server
     *
     * @param config : the configuration object for this app
     * @return boolean: whether all is good. If returns false usually means that location monitoring isn't on due
     * to permission problem or other.  You should ensure that the onRequestPermission override is defined to call ensure monitoring.
     **/

    public boolean start(@NonNull TesConfig config) {
        // check to see if we have already called this and just return.
        if (hasInit()) {
            this.locMgr.ensureMonitoring();
            return true;
        }

        this.config = config;

        try {
            this.config.saveConfig(this.wiCtx);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save configuration due to invalid JSON object");
            e.printStackTrace();
        }

        // setup the managers
        this.locMgr = new TesLocationMgr(this.wiActivity, this.wiCtx, this.mViewId, this.config);
        this.locMgr.listener = this;

        this.pushMgr = new TesPushMgr(this.config.getEnvPushProfile(), this.wiCtx, this.config);
        this.pushMgr.listener = this;

        this.walletMgr = new TesWalletMgr();

        // setup the API caller
        this.api = new TesApi(this.wiCtx);
        this.api.authListener = this;
        this.api.setEndpoint(this.config.getEnvServer());

        this.initTokens();
        this.setLocaleAndVersionInfo();

        Log.i(TAG, String.format("Environment: %s Debug: %b Endpoint: %s", config.environment, config.debug, this.api.getEndpoint()));

        // get our push token if we want to register GCM devices
        this.reRegisterServices();

        // we self authenticate and pass on the result to any delgate implementing this routine.
        if (!this.isAuthorized() && this.config.authAutoAuthenticate) {
            this.authenticate(this.config.authCredentials, new TesApi.TesApiListener() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (TesWIApp.this.listener != null) {
                        TesWIApp.this.listener.onAutoAuthenticate(TesApi.TESCallSuccessOK, result, null);
                    }
                }

                @Override
                public void onFailed(JSONObject result) {
                    if (TesWIApp.this.listener != null) {
                        TesWIApp.this.listener.onAutoAuthenticate(TesApi.TESCallSuccessFAIL, result, null);
                    }
                }

                @Override
                public void onOtherError(TesApiException error) {
                    if (TesWIApp.this.listener != null) {

                        TesWIApp.this.listener.onAutoAuthenticate(TesApi.TESCallError, null, error);
                    }
                }
            });
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        Intent intent = this.wiActivity.getIntent();
        if (intent.getExtras() != null) {

            TesDictionary dict = new TesDictionary();
            Log.d(TAG, "Launched from notification message");
            for (String key : intent.getExtras().keySet()) {
                Object value = intent.getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
                dict.put(key, value);
            }
            JSONObject data = new JSONObject(dict);

            String notifyType = data.optString("notifyType");
            if (notifyType != null  && notifyType.equalsIgnoreCase(TesWalletMgr.WALLET_NOTIFICATION)){
                this.walletMgr.saveToAndroid(this.wiActivity, data.optJSONObject("payload"));
                if (this.listener != null)
                    this.listener.onWalletNotification(data);
            }
            else {
                if (this.listener != null)
                    this.listener.onRemoteDataNotification(data);
            }
        }

        boolean monitoring = this.locMgr.start(this.config.requireBackgroundLocation);

        setInitDone();
        return monitoring;
    }

    /**
     * Start the app but only doing enough to kick the api so we can make network calls. This
     * is typically called when a pending intent starts
     *
     * @param config the config object
     * @return true for success / false otherwise
     */
    public boolean startApi(@NonNull TesConfig config) {
        if (hasInit() && this.api != null){
            return true;
        }

        this.config = config;

        // setup the API caller
        this.api = new TesApi(this.wiCtx);
        this.api.authListener = this;
        this.api.setEndpoint(this.config.getEnvServer());

        this.initTokens();
        this.setLocaleAndVersionInfo();

        Log.i(TAG, String.format("Environment: %s Debug: %b Endpoint: %s", config.environment, config.debug, this.api.getEndpoint()));

        return true;

    }

    /**
     * Start the app location manager only. Used in background services to do location related bits
     *
     * @param config   the config object
     * @param listener the loc manager listener.
     * @return true for success / false otherwise
     */
    public boolean startLocationMgr(@NonNull TesConfig config, @Nullable TesLocationMgr.TesLocationMgrListener listener) {
        boolean monitoring = false;
        if (hasInit() && this.locMgr != null){
            monitoring = this.locMgr.ensureMonitoring();
            if (monitoring && listener != null){
                if (this.locMgr.isSuspended())
                    listener.onConnectionSuspended(0);
                else
                    listener.onConnected(null);
            }
            return monitoring;
        }

        this.config = config;

        this.initTokens();
        this.setLocaleAndVersionInfo();
        
        // setup just the location manager
        this.locMgr = new TesLocationMgr(this.wiActivity, this.wiCtx, this.mViewId, this.config);
        this.locMgr.listener = listener;
        monitoring = this.locMgr.start(this.config.requireBackgroundLocation);
        return monitoring;
    }

    /**
     * Should be added on a request permission callback to ensure that monitoring is restarted if the user gives
     * permission
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void startOnRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        this.locMgr.processRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * returns whether the user has a valid device token
     */

    public boolean hasDeviceToken() {
        return this.deviceToken != null && (this.deviceToken.length() > 0);
    }

    /**
     * returns whether the user has a valid push token
     */
    boolean hasPushToken() {
        return this.pushToken != null && (this.pushToken.length() > 0);
    }


    /**
     * are we authorized
     */
    boolean isAuthorized() {
        return this.api.isAuthorized();
    }

    /**
     * Clears the set of auth tokens
     */
    public void clearAuth() {
        this.clearToken(this.ACCESS_AUTH_TYPE);
        this.clearToken(this.ACCESS_USER_NAME);
        this.clearToken(this.ACCESS_TOKEN_KEY);
        this.clearToken(this.ACCESS_USER_SETTINGS);
    }

    /**
     * register services
     * NOTE: only needed if you turn on gcm or wallet or other device type after calling start
     */

    public void reRegisterServices() {
        if ((this.config.deviceTypes & TesConfig.deviceTypeFCM) == TesConfig.deviceTypeFCM)
            this.pushMgr.registerRemoteNotifications();
            String token = this.pushMgr.getToken();
            if (token != null){
                this.setToken(PUSH_TOKEN_KEY, token);
            }

    }

    //-----------------------
    // Interface calls
    //----------------------

    @Override
    public void httpAuthFailure(NetworkResponse response) {
        if (this.config.authAutoAuthenticate) {
            this.authenticate(this.config.authCredentials, new TesApi.TesApiListener() {
                @Override
                public void onSuccess(JSONObject result) {

                }

                @Override
                public void onFailed(JSONObject result) {

                }

                @Override
                public void onOtherError(TesApiException error) {
                    if (error.getCause() != null && error.getCause().getClass().isInstance(VolleyError.class)) {
                        VolleyError realError = (VolleyError) error.getCause();
                        if (TesWIApp.this.api.isAuthFailure(realError)) {
                            if (TesWIApp.this.listener != null) {
                                NetworkResponse response1 = realError.networkResponse;
                                if (response1 != null) {
                                    TesWIApp.this.listener.authorizeFailure(response1.statusCode,
                                            response1.data,
                                            response1.notModified,
                                            response1.networkTimeMs,
                                            response1.headers);
                                }
                            }
                        }
                    }
                }
            });
        } else {
            if (this.listener != null) {
                this.listener.authorizeFailure(
                        response.statusCode,
                        response.data,
                        response.notModified,
                        response.networkTimeMs,
                        response.headers);
            }
        }
    }

    /**
     * Sends a point to the server. Automatically deals with new or existing devices
     *
     * @param locInfo      the location info to update
     * @param inBackground are we running in background atm.
     */

    @Override
    public void sendDeviceUpdate(@Nullable final TesLocationInfo locInfo, final boolean inBackground, final TesApi.TesApiListener listener) throws JSONException {

        String path = null;

        final TesDevice dev = this._fillDeviceFromLocation(locInfo);
        JSONObject parameters = dev.toDictionary();

        TesApi.TesApiListener callback = new TesApi.TesApiListener() {
            @Override
            public void onSuccess(JSONObject result) {
                // the devuce is returned on a post call only
                String device_id = result.optString("device_id", null);
                if (device_id != null && device_id.length() > 0) {
                    TesWIApp.this.setToken(DEVICE_TOKEN_KEY, device_id);
                }
                if (listener != null)
                    listener.onSuccess(result);

            }

            @Override
            public void onFailed(JSONObject result) {
                try {
                    int code = result.optInt("code", Integer.MIN_VALUE);
                    String msg = result.getString("msg");
                    if (code != Integer.MIN_VALUE && code == ERROR_NOT_FOUND) {
                        // looks like the device id has been nuked on the server. Just try again with a new device id
                        Log.i(TAG, "Trying again with no device token");
                        TesWIApp.this.clearToken(DEVICE_TOKEN_KEY);
                        TesWIApp.this.sendDeviceUpdate(locInfo, inBackground, null);

                    } else {
                        TesUtils.writeDebugMsg(String.format("Device token request failed with unknown code: %s  (inbackground %b)", msg, inBackground));
                        if (listener != null)
                            listener.onFailed(result);
                    }
                } catch (JSONException e) {
                    this.onOtherError(new TesApiException("Failed to get error details for device creation, invalid response", e));

                }
            }

            @Override
            public void onOtherError(TesApiException error) {
                Log.i(TAG, String.format("Request Failed: %s. %s", error.toString(), error.getCause().toString()));
                TesUtils.writeDebugMsg(String.format("Request failed : %s, %s", error.toString(), error.getCause().toString()));
                if (listener != null)
                    listener.onOtherError(error);
            }
        };

        if (this.deviceToken != null) {
            path = String.format("%s/%s", TES_PATH_GEODEVICE, this.deviceToken);
            this.api.call(Request.Method.PUT, path, parameters, callback, true);
        } else {
            path = TES_PATH_GEODEVICE;
            this.api.call(Request.Method.POST, path, parameters, callback, true);
        }

        // TODO: does android have an equivalent
       /*
       if (background){
            __weak __typeof(&*self)weakSelf = self;
       [self.api setShouldExecuteAsBackgroundTask:task WithExpirationHandler:^{
                __strong __typeof(&*weakSelf)strongSelf = weakSelf;
            [strongSelf.locMgr writeDebugMsg:nil msg:[NSString stringWithFormat:@"HTTP Request background task terminated"]];
            }];
        }
        */

    }


    @Override
    public void onRefreshToken(String token) {
        if (token != null){
            this.setToken(PUSH_TOKEN_KEY, token);
            // TODO: PJF - may need to send an update to the device here. For
            // now just set the token, and the next loc update should fix things.
        }
        if (this.listener!=null)
            this.listener.onRefreshToken(token);
    }

    @Override
    public void onDataMsg(JSONObject data) {
        String notifyType = data.optString("notifyType");
        if (notifyType != null  && notifyType.equalsIgnoreCase(TesWalletMgr.WALLET_NOTIFICATION)){
            this.walletMgr.saveToAndroid(this.wiActivity, data.optJSONObject("payload"));
            if (this.listener != null)
                this.listener.onWalletNotification(data);
        }
        else {
            if (this.listener != null)
                this.listener.onRemoteDataNotification(data);
        }
    }

    @Override
    public void onNotificationMsg(JSONObject data) {
        String notifyType = data.optString("notifyType");
        if (notifyType != null  && notifyType.equalsIgnoreCase(TesWalletMgr.WALLET_NOTIFICATION)){
            this.walletMgr.saveToAndroid(this.wiActivity, data.optJSONObject("payload"));
            if (this.listener != null)
                this.listener.onWalletNotification(data);
        }
        else {
            if (this.listener != null)
                this.listener.onRemoteNotification(data);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        this.locMgr.requestLocationUpdates();
        // ensure that we generate the last known location
        this.locMgr.sendLastKnownLocation();

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, String.format("Connection Suspended: %d" , i));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, String.format("Connection failed: %s" , connectionResult.toString()));
    }

    @Override
    public void onError(@Nullable String msg) {

    }

    //----------------------
    // Geofencing calls
    //----------------------

    public void addGeofence(double latitude, double longitude)
    {
        this.locMgr.addGeofence(latitude, longitude);
    }

    public void removeGeofence()
    {
        this.locMgr.removeGeofence();
    }

    //-----------------------
    // WIApp API calls
    //-----------------------

    /**
     * Authenticate a session
     * @param params params to pass to register/login
     * @param listener call on function result
     */
   public void authenticate(JSONObject params, TesApi.TesApiListener listener) {

       if (this.isAuthenticating())
           return; // already doing an authenticate

       Boolean haveUser = false;
       if (params == null){
           params = new JSONObject();
           try {
               if (this.authUserName != null && this.api.accessAuthType.equals(TesApi.TES_AUTH_TYPE_ANONYMOUS)){
                   params.put("user_name", this.authUserName);
               }
                else {
                       params.put("anonymous_user", true);
               }
           } catch (JSONException e) {
               e.printStackTrace();
           }

       }

       if (params.has("user_name")){
           haveUser = true;
       }


       if (haveUser){
          this.loginUser(params, this._wrapLoginListener(listener));
       }
       else {
        this.registerUser(params, this._wrapLoginListener(listener));
       }
   }

   private TesApi.TesApiListener _wrapLoginListener(final TesApi.TesApiListener inner){
       return new TesApi.TesApiListener() {
           @Override
           public void onSuccess(JSONObject result){
               try {
                   TesWIApp.this._finishAuth(result);
                    inner.onSuccess(result);
               } catch (JSONException e) {
                   this.onOtherError(new TesApiException("Failed to parse result", e));
               }
           }

           @Override
           public void onFailed(JSONObject result) {
               TesWIApp.this.clearAuth();
               inner.onFailed(result);
           }

           @Override
           public void onOtherError(TesApiException error) {
               // something went wrong don't do anything - could be bas connection
               inner.onOtherError(error);
           }

       };
   }

   private void _finishAuth(@Nullable JSONObject responseObject) throws JSONException{
       if (!this.hasDeviceToken()){
           this.sendDeviceUpdate(null, false, new TesApi.TesApiListener() {
               @Override
               public void onSuccess(JSONObject result) {
                   TesWIApp.this.locMgr.ensureMonitoring();
               }

               @Override
               public void onFailed(JSONObject result) {
                   TesWIApp.this.locMgr.ensureMonitoring();
               }

               @Override
               public void onOtherError(TesApiException error) {
                   TesWIApp.this.locMgr.ensureMonitoring();
               }
           });
       }
       else {
           this.locMgr.ensureMonitoring();
       }

   }

   void _loginOrRegister(Integer method, String path,  JSONObject params, final TesApi.TesApiListener listener)
   {
       if (this.isAuthenticating()){
           if (listener != null){
               listener.onOtherError(new TesApiException("Already in authenticate call"));
           }
           return;
       }

       this.setAuthenticating(true);
        this.api.call(method, path, params, new TesApi.TesApiListener() {
            @Override
            public void onSuccess(JSONObject result) {
                TesWIApp.this.setAuthenticating(false);
                JSONObject data = null;
                try {
                    data = result.getJSONObject("data");

                    String token_id = data.getString("token");
                    String auth_type = data.getString("auth_type");
                    String user_name = data.getString("user_name");

                    if (token_id != null) {
                        TesWIApp.this.setToken(ACCESS_TOKEN_KEY, token_id);
                        TesWIApp.this.setToken(ACCESS_AUTH_TYPE, auth_type);
                        TesWIApp.this.setToken(ACCESS_USER_NAME, user_name);
                        TesWIApp.this.api.setAccessToken(token_id);
                    }
                } catch (JSONException e) {
                    this.onOtherError(new TesApiException("Failed to parse result", e));
                }
                if (listener != null)
                    listener.onSuccess(result);
            }

            @Override
            public void onFailed(JSONObject result) {
                TesWIApp.this.setAuthenticating(false);
                if (listener != null)
                    listener.onFailed(result);
            }

            @Override
            public void onOtherError(TesApiException error) {
                TesWIApp.this.setAuthenticating(false);
                if (listener != null)
                    listener.onOtherError(error);

            }
        }, false);
   }

    /**
     * register a user

     To create an anonymous user pass in anonymous_user=true

     @param params Parameters to the list live events call
     @param listener the code block to call on  completion
     */

   public void registerUser(JSONObject params, final TesApi.TesApiListener listener) {
       this._loginOrRegister(Request.Method.POST, TES_PATH_REGISTER, params, listener);
   }


    /**
     login a user

     @param params Parameters to the list live events call
     @param listener the code block to call on  completion
     **/

   public void loginUser(JSONObject params,  TesApi.TesApiListener listener) {
       this._loginOrRegister(Request.Method.POST, TES_PATH_LOGIN, params, listener);
   }


    /**
     account profile for user

     must be registered or logged in

     **/

   public void getAccountProfile(TesApi.TesApiListener listener) {
       this.api.call(Request.Method.GET, TES_PATH_PROFILE, null, listener, true);
   }

    /**
     update account profile for user

     must be registered or logged in

     @param listener the code block to call on  completion
     **/

   public void updateAccountProfile(JSONObject params, TesApi.TesApiListener listener) {
       this.api.call(Request.Method.PUT, TES_PATH_PROFILE, params, listener, true);
   }


    /**
     update account profile password for user

     only if user is not anonymous

     @param password - the new password
     @param oldPassword - the old password
     @param listener the code block to call on  completion
     **/

   public void updateAccountProfilePassword(String password, String oldPassword, TesApi.TesApiListener listener) throws JSONException {
       JSONObject params = new JSONObject();
       params.put("password", password);
       params.put("old_password", oldPassword);
       this.api.call(Request.Method.PUT, TES_PATH_PROFILE, params, listener, true);

   }

    /**
     update account profile setting for user

     @param params - settings to change can be  exclusions, following, watch_zones, notifications, allow_notifications
     @param listener the code block to call on successful
     **/
   public void updateAccountSettings(JSONObject params, TesApi.TesApiListener listener) throws JSONException {
       JSONObject new_settings = this.getJsonToken(ACCESS_USER_SETTINGS, null);
       if (new_settings == null) {
           new_settings = new JSONObject();
           new_settings.put("following", new JSONArray());
           new_settings.put("watch_zones", new JSONArray());
           new_settings.put("notifications", new JSONObject());
           new_settings.put( "exclusions", new JSONObject());

       }

       JSONObject settings = new JSONObject();
       settings.put("settings", TesUtils.mergeJSONObjects(new_settings, params));
       this.api.call(Request.Method.PUT, TES_PATH_PROFILE, settings, listener, true);
   }


    /**
     upload an image for a user profile

     @param image the image to upload
     @param listener the completion block to execute on completion, returns the new image id from the server
     **/

   public void uploadImageForProfile(Bitmap image, TesApi.TesApiListener listener) {
       // TODO: sort out how to do this
   }

    /**
     reads an image into a user object

     @param listener the completion block to execute on completion, returns the new image id from the server
     **/

   public void readProfileImage(ImageLoader.ImageListener listener) {
       ImageLoader loader = this.api.getImageLoader();
       loader.get(TES_PATH_ACCOUNT_PROFILE_IMAGES, listener);
   }

    /**
     List all live events for a device token

     @param params Parameters to the list live events call
     @param listener the code block to call on  completion
     **/

   public void listLiveEvents(JSONObject params, TesApi.TesApiListener listener) {
       this.api.call(Request.Method.GET, TES_PATH_LIVE_EVENTS, params, listener, true);
   }

    /**
     List all search events for a location and distance

     @param params Parameters to the list search events call -
     distance = distance to search
     units = metric/imperial
     num = number of results to return , 0 for all
     @param listener the code block to call on successful completion
     **/

   public void listSearchEvents(JSONObject params, Location location , TesApi.TesApiListener listener)
   {
       String path = String.format(TES_PATH_SEARCH_LIVE_EVENTS, location.getLongitude(), location.getLatitude());
       this.api.call(Request.Method.GET, path, params, listener, true);

   }


    /**
     List all acknowledged live events

     @param params Parameters to the list live events call
     @param listener the code block to call on successful completion
     **/

   public void listAcknowledgedLiveEvents(JSONObject params, TesApi.TesApiListener listener) {
      this.api.call(Request.Method.GET, TES_PATH_LIST_LIVE_EVENTS_ACK, params, listener, true);

   }

    /**
     List all followed live events

     @param params Parameters to the list live events call
     @param listener the code block to call on successful completion
     **/
   public void listFollowedLiveEvents(JSONObject params, TesApi.TesApiListener listener) {
       this.api.call(Request.Method.GET, TES_PATH_LIST_LIVE_EVENTS_FOLLOW , params, listener, true);
   }

    /**

     update the event acknoledge flag for an event

     @param eventId Event to ack/non-ack
     @param ack either YES or NO to ack or not ack
     @param listener the code block to call on successful completion.

     **/

   public void updateEventAck(String eventId, boolean ack, TesApi.TesApiListener listener) throws JSONException{
       JSONObject params = new JSONObject();
       params.put("ack", ack);
       String path = String.format(TES_PATH_LIVE_EVENTS_ACK, eventId);
       this.api.call(Request.Method.PUT, path, params, listener, true);
   }


    /**

     update the event enacted flag for an event

     @param eventId Event to enact=
     @param enacted YES or NO to enact or not
     @param listener the code block to call on successful completion.

     **/

   public void updateEventEnacted(String eventId, boolean enacted, TesApi.TesApiListener listener) throws JSONException{
       JSONObject params = new JSONObject();
       params.put("enact", enacted);
       String path = String.format(TES_PATH_LIVE_EVENTS_ENACTED, eventId);
       this.api.call(Request.Method.PUT, path, params, listener, true);
   }


    /**
     reads an image into an event object

     @param imageId - the poi object to read to
     @param listener - completion block to call on success
     **/
    public void readEventImage(String imageId, ImageLoader.ImageListener listener) {
        String path = String.format(TES_PATH_ANY_IMAGES_RUD, imageId);
        ImageLoader loader = this.api.getImageLoader();
        loader.get(path, listener);
    }

    /**

     Get current provider linked to this app

     @param listener the code block to call on successful completion.

     */

   public void getProvider(TesApi.TesApiListener listener) throws JSONException {
       String path = String.format(TES_PATH_PROVIDER, this.providerToken);
       JSONObject params = new JSONObject();
       params.put("timezone", this.timezoneToken);
       params.put("locale", this.localeToken);
       this.api.call(Request.Method.GET, path, params, listener, true);
   }

    /**
     List all pois for a provider

     @param params Extra params to send to the call
     @param listener the code block to call on successful completion.

     **/
   public void listPlacesOfInterestForProvider(JSONObject params, TesApi.TesApiListener listener) {
       String path = String.format(TES_PATH_POI, this.providerToken);
       this.api.call(Request.Method.GET, path, params, listener, true);
   }

    /**
     List all live events created by a provider

     @param params Extra params to send to the call
     @param listener the code block to call on successful completion.

     **/
   public void listLiveEventsForProvider(JSONObject params, TesApi.TesApiListener listener) {
       String path = String.format(TES_PATH_PROVIDER_LIVE_EVENTS, this.providerToken);
       this.api.call(Request.Method.GET, path, params, listener, true);
   }

    /**
     List all historic events for a rrovider

     @param params Extra params to send to the call
     @param listener the code block to call on successful completion.

     **/
   public void listEventsForProvider(JSONObject params, TesApi.TesApiListener listener) {
       String path = String.format(TES_PATH_PROVIDER_EVENTS, this.providerToken);
       this.api.call(Request.Method.GET, path, params, listener, true);
   }

    /**
     List all reviews for a place

     @param params Extra params to send to the call
     @param listener the code block to call on successful completion.

     **/
   public void listReviewsForPlace(String poiId, JSONObject params, TesApi.TesApiListener listener) {
       String path = String.format(TES_PATH_PLACE_REVIEWS, poiId);
       this.api.call(Request.Method.GET, path, params, listener, true);
   }


    /**

     create a review for a place
     @param poiId Place to list reviews for
     @param params Extra params to send to the call
     @param listener the code block to call on successful completion.

     **/

    public void createReviewForPlace(String poiId, JSONObject params, TesApi.TesApiListener listener) {
        String path = String.format(TES_PATH_PLACE_CREATE_REVIEW, poiId);
        this.api.call(Request.Method.POST, path, params, listener, true);
    }

    /**
     update an existing review

     @param reviewId to update
     @param params all the poi parameters
     @param listener the completion block to execute on completion, returns and updated poi dictionary from the server
     **/
   public void updateReview(String reviewId, JSONObject params, TesApi.TesApiListener listener) {
       String path = String.format(TES_PATH_REVIEW_RUD, reviewId);
       this.api.call(Request.Method.PUT, path, params, listener, true);
   }

    /**
     delete an existing review

     @param reviewId to delete
     @param listener the completion block to execute on completion, returns and updated poi dictionary from the server
     **/
   public void  deleteReview(String reviewId, TesApi.TesApiListener listener) {
       String path = String.format(TES_PATH_REVIEW_RUD, reviewId);
       this.api.call(Request.Method.DELETE, path, null, listener, true);
   }


    /**
     reads an image into a poi object

     @param imageId - the poi object to read to
     @param listener - completion block to call on success
     **/
     public void readPoiImage(String imageId, ImageLoader.ImageListener listener) {
         String path = String.format(TES_PATH_ANY_IMAGES_RUD, imageId);
         ImageLoader loader = this.api.getImageLoader();
         loader.get(path, listener);
     }



    //-----------------------------------------------------
    // Utility functions
    //-----------------------------------------------------

   public @NonNull TesDevice _fillDeviceFromLocation(@Nullable TesLocationInfo loc) throws JSONException
   {
       if (loc == null)
            loc = TesLocationInfo.createEmptyLocation();

       JSONObject currentLoc = new JSONObject();
       currentLoc.put("current", loc.toDictionary());
       TesDevice device = new TesDevice(currentLoc);

        JSONArray pushTargets = new JSONArray();
        TesPushInfo pushInfo = null;
        if ((this.config.deviceTypes & TesConfig.deviceTypeFCM) == TesConfig.deviceTypeFCM){
            // setup the push token if its new or we are a new device.
            if (this.pushToken != null){
                pushInfo = new TesPushInfo(TesConfig.DEVICE_TYPE_FCM, this.pushToken, this.config.getEnvPushProfile());
                pushTargets.put(pushInfo.toDictionary());
            }

        }

        if ((this.config.deviceTypes & TesConfig.deviceTypeWallet) == TesConfig.deviceTypeWallet ){
            pushInfo = new TesPushInfo(TesConfig.DEVICE_TYPE_WALLET, TesConfig.WALLET_PROFILE, this.config.walletOfferClass);
            pushTargets.put(pushInfo.toDictionary());
        }

        if ((this.config.deviceTypes & TesConfig.deviceTypeMail) == TesConfig.deviceTypeMail){
            pushInfo = new TesPushInfo(TesConfig.DEVICE_TYPE_MAIL, TesConfig.MAIL_PROFILE, "");
            pushTargets.put(pushInfo.toDictionary());
        }

        if ((this.config.deviceTypes & TesConfig.deviceTypeSms) == TesConfig.deviceTypeSms){
            pushInfo = new TesPushInfo(TesConfig.DEVICE_TYPE_SMS, TesConfig.SMS_PROFILE, "");
            pushTargets.put(pushInfo.toDictionary());
        }

        int tgtCount = pushTargets.length();
        if (tgtCount == 1) {
            // setup the push token if its new or we are a new device.
            pushInfo = new TesPushInfo(pushTargets.getJSONObject(0));
            device.pushToken = pushInfo.pushToken;
            device.pushType = pushInfo.pushType;
            device.pushProfile = pushInfo.pushProfile;
        }
        else if (tgtCount > 1){
            device.pushType = TesConfig.DEVICE_TYPE_MULTIPLE;
            device.pushToken = "";
            device.pushProfile = "";
            device.pushTargets = pushTargets;
        }

        if (this.localeToken != null){
            device.locale = this.localeToken;
        }
        if (this.timezoneToken != null){
            device.timezoneOffset = this.timezoneToken;
        }
        if (this.versionToken != null ){
            device.version = this.versionToken;
        }

        return device;
    }

    //------------------------------------------------------
    // reads/updates/clears token to the user defaults store
    //------------------------------------------------------

   public void initTokens(){
       //setup the special tokens we keep track of

       this.api.accessToken = this.getToken(ACCESS_TOKEN_KEY);
       this.api.accessAuthType = this.getToken(ACCESS_AUTH_TYPE);
       this.api.accessSystemDefaults = this.getJsonToken(ACCESS_SYSTEM_DEFAULTS,null);

       this.deviceToken = this.getToken(DEVICE_TOKEN_KEY);
       this.pushToken = this.getToken(PUSH_TOKEN_KEY);
       this.localeToken = this.getToken(LOCALE_TOKEN_KEY);
       this.timezoneToken = this.getToken(TIMEZONE_TOKEN_KEY);

       this.versionToken = this.getToken(LAST_LAUNCHED_VERSION_TOKEN_KEY);

       // clear out the new flags
       this.newPushToken = false;
       this.newDeviceToken = false;
       this.newAccessToken = false;
       this.newAccessAuthType = false;

       this.newLocaleToken = false;
       this.newTimezoneToken = false;
       this.newVersionToken = false;
   }

   public void setLocaleAndVersionInfo(){
       Locale locale = Locale.getDefault();
       String currentLocaleID = locale.getLanguage();

       TimeZone tz = TimeZone.getDefault();
       Date now = new Date();

       String version =  BuildConfig.VERSION_NAME;

       this.setToken(LOCALE_TOKEN_KEY, currentLocaleID);
       this.setToken(TIMEZONE_TOKEN_KEY, String.format("%s", tz.getOffset(now.getTime())/1000));
       this.setToken(LAST_LAUNCHED_VERSION_TOKEN_KEY, version);
   }

    String getToken(String token){
        return this.sharedPrefs.getString(token, null);
    }

    String getToken(String token, String defaultValue){
        return this.sharedPrefs.getString(token, defaultValue);
    }

    JSONObject getJsonToken(String token, JSONObject defaultValue){
        try {
            String json = this.sharedPrefs.getString(token, null);
            if (json == null)
                return defaultValue;

            return new JSONObject(json);
        }
        catch (JSONException e){
            return defaultValue;
        }
    }

   public void setToken(String token, String value){
       SharedPreferences.Editor edit = this.sharedPrefs.edit();
       edit.putString(token, value);
       edit.commit();
       _syncInternalTokens(token, value);
   }

   public void setToken(String token, JSONObject value){
       SharedPreferences.Editor edit = this.sharedPrefs.edit();
       String json = value.toString();
       edit.putString(token, json);
       edit.commit();
       _syncInternalTokens(token, value);
   }

   public void clearToken(String  token){
       SharedPreferences.Editor edit = this.sharedPrefs.edit();
       edit.remove(token);
       edit.commit();
       _syncInternalTokens(token, null);
   }

   private void _syncInternalTokens(String token, Object value) {
        if(token.equals(ACCESS_TOKEN_KEY)){
            newAccessToken =  (this.api.accessToken == null && value != null) || (!this.api.accessToken.equals(value));
            this.api.accessToken = (String) value;
        }
        else if (token.equals(ACCESS_AUTH_TYPE)){
            newAccessAuthType =  (this.api.accessAuthType == null && value != null) || !this.api.accessAuthType.equals(value);
            this.api.accessAuthType = (String) value;
        }
        else if (token.equals(ACCESS_SYSTEM_DEFAULTS)){
            newAccessSystemDefaults = true;
            this.api.accessSystemDefaults = (JSONObject) value;
        }
        else if (token.equals(DEVICE_TOKEN_KEY)){
            newDeviceToken =  (this.deviceToken == null && value != null) || !this.deviceToken.equals(value);
            this.deviceToken = (String) value;
        }
        else if (token.equals(PUSH_TOKEN_KEY)){
            newPushToken =  (this.pushToken == null && value != null) || !this.pushToken.equals(value);
            this.pushToken = (String) value;
        }
        else if (token.equals(LOCALE_TOKEN_KEY)){
            newLocaleToken =  (this.localeToken == null && value != null) || !this.localeToken.equals(value);
            this.localeToken = (String)  value;
        }
        else if (token.equals(TIMEZONE_TOKEN_KEY)){
            newTimezoneToken =  (this.timezoneToken == null && value != null) || !this.timezoneToken.equals(value);
            this.timezoneToken = (String) value;
        }
        else if (token.equals(LAST_LAUNCHED_VERSION_TOKEN_KEY)){
            newVersionToken = (this.versionToken == null && value !=null) || !this.versionToken.equals(value);
            this.versionToken = (String) value;
        }
        else if (token.equals(ACCESS_USER_NAME)){
            newAccessUserName = (this.authUserName == null && value != null) || !this.authUserName.equals(value);
            this.authUserName = (String) value;
        }

       if (newAccessToken && this.listener !=  null){
            this.listener.newAccessToken(this.api.accessToken);
            newAccessToken = false;
        }

       if (newDeviceToken && this.listener !=  null){
            this.listener.newDeviceToken(this.deviceToken);
            newDeviceToken = false;
        }

       if (newPushToken && this.listener !=  null){
            this.listener.newPushToken(this.pushToken);
            newPushToken = false;
        }
    }

    //--------------------------------------------------------------
    // helpers to make it easier to get/set a particular user setting
    //---------------------------------------------------------------

   String  getUserSetting(String  key){
       JSONObject  user_settings =this.getJsonToken(ACCESS_USER_SETTINGS, null);
       if (user_settings == null)
           return null;

       String val = null;
       try {
           val = user_settings.getString(key);
       } catch (JSONException e) {
           Log.i(TAG, String.format("Invalid user setting: %s : %s", key, e.toString()));
       }
       if (val == null)
           return null;

       return val;
   }

    JSONObject getUserSettingJson(String  key){
        JSONObject  user_settings =this.getJsonToken(ACCESS_USER_SETTINGS, null);
        if (user_settings == null)
            return null;

        JSONObject val = null;
        try {
            val = user_settings.getJSONObject(key);
        } catch (JSONException e) {
            Log.i(TAG, String.format("Invalid user setting: %s : %s", key, e.toString()));
        }
        if (val == null)
            return null;

        return val;
    }

    JSONArray  getUserSettingJsonArray(String  key) {
        JSONObject user_settings = this.getJsonToken(ACCESS_USER_SETTINGS, null);
        if (user_settings == null)
            return null;

        JSONArray val = null;
        try {
            val = user_settings.getJSONArray(key);
        } catch (JSONException e) {
            Log.i(TAG, String.format("Invalid user setting: %s : %s", key, e.toString()));
        }
        if (val == null)
            return null;

        return val;
    }

    public void setUserSetting(String  key, String value){
       JSONObject  user_settings =this.getJsonToken(ACCESS_USER_SETTINGS, null);
       if (user_settings == null)
           user_settings = new JSONObject();

       try {
           user_settings.put(key, value);
           this.setToken(ACCESS_USER_SETTINGS, user_settings);
       } catch (JSONException e) {
           Log.i(TAG, String.format("Unable to set user setting: %s : %s", key, e.toString()));
       }

   }

    public void setUserSetting(String  key, JSONObject value){
        JSONObject  user_settings =this.getJsonToken(ACCESS_USER_SETTINGS, null);
        if (user_settings == null)
            user_settings = new JSONObject();

        try {
            user_settings.put(key, value);
            this.setToken(ACCESS_USER_SETTINGS, user_settings);
        } catch (JSONException e) {
            Log.i(TAG, String.format("Unable to set user setting: %s : %s", key, e.toString()));
        }

    }

    public void setUserSetting(String  key, JSONArray value){
        JSONObject  user_settings =this.getJsonToken(ACCESS_USER_SETTINGS, null);
        if (user_settings == null)
            user_settings = new JSONObject();

        try {
            user_settings.put(key, value);
            this.setToken(ACCESS_USER_SETTINGS, user_settings);
        } catch (JSONException e) {
            Log.i(TAG, String.format("Unable to set user setting: %s : %s", key, e.toString()));
        }

    }
    // excludes support

    public JSONObject getExclusions() {
        return this.getUserSettingJson("exclusions");
    }

    public int findExclusionIndex(String name, JSONArray exclusions){
        return TesUtils.findJsonArrayIndex(name, exclusions);
    }


    /**
     * Exclude an event category for a particular event type (*) for all. (ie. add to exclusion list)
     * @param exc_type event type
     * @param exc_cat  event cat or * for all
     * @param listener callback handler
     * @throws JSONException shouldn't happen
     */
    public void excludeEventType(String exc_type, String exc_cat, final TesApi.TesApiListener listener) throws JSONException {
       // update the exclusion dictionary
       JSONObject exclusions = this.getUserSettingJson("exclusions");
       if (exclusions == null)
           exclusions = new JSONObject();

       JSONArray exclusion_list = exclusions.optJSONArray(exc_type);
       if (exclusion_list == null)
           exclusion_list = new JSONArray();

        int idx = this.findExclusionIndex(exc_cat, exclusion_list);
        if (idx < 0){
            exclusion_list.put(exc_cat);
        }
        exclusions.put(exc_type, exclusion_list);

        JSONObject params = new JSONObject();
        params.put("exclusions", exclusions);

        final JSONObject exlusionFinal = exclusions;
        this.updateAccountSettings(params, new TesApi.TesApiListener() {
            @Override
            public void onSuccess(JSONObject result) {
                TesWIApp.this.setUserSetting("exclusions", exlusionFinal);
                if (listener != null)
                    listener.onSuccess(result);
            }

            @Override
            public void onFailed(JSONObject result) {
                if (listener != null)
                    listener.onFailed(result);
            }

            @Override
            public void onOtherError(TesApiException error) {
                if (listener != null)
                    listener.onOtherError(error);
            }
        });
   }

    /**
     * Include an event category for a particular event type (*) for all. (ie. remove to exclusion list)
     * @param evt_type event type
     * @param evt_cat  event cat or * for all
     * @param listener callback handler
     * @throws JSONException shouldn't happen
     */

    public void includeEventType(String evt_type, String evt_cat, final TesApi.TesApiListener listener) throws JSONException {

       // update the exclusion dictionary
       JSONObject exclusions = this.getUserSettingJson("exclusions");
       if (exclusions == null){
           if (listener != null)
               listener.onOtherError(new TesApiException("No exclusions found"));
           return;
       }

       JSONArray exclusion_list = exclusions.optJSONArray(evt_type);
       if (exclusion_list == null){
          if (listener != null)
               listener.onOtherError(new TesApiException("No exclusions for event type found"));
           return;
       }

       boolean changed = false;
       int idx = this.findExclusionIndex(evt_cat, exclusion_list);
       if (idx >= 0){
           exclusion_list.remove(idx);
           changed = true;
       }

       if (changed){
           if (exclusion_list.length()<1){
               exclusions.remove(evt_type);
           }
            else {
                exclusions.put(evt_type, exclusion_list);
            }

           JSONObject params = new JSONObject();
           params.put("exclusions", exclusions);

           final JSONObject exlusionFinal = exclusions;
           this.updateAccountSettings(params, new TesApi.TesApiListener() {
               @Override
               public void onSuccess(JSONObject result) {
                   TesWIApp.this.setUserSetting("exclusions", exlusionFinal);
                   if (listener != null)
                       listener.onSuccess(result);
               }

               @Override
               public void onFailed(JSONObject result) {
                   if (listener != null)
                       listener.onFailed(result);
               }

               @Override
               public void onOtherError(TesApiException error) {
                   if (listener != null)
                       listener.onOtherError(error);
               }
           });
       }
       else {
           if (listener != null)
               listener.onOtherError(new TesApiException("No changes to exclusion list found"));
       }
   }

    // notify support
   public void allowNotifyEventType(String  evt_type, String  evt_cat, final TesApi.TesApiListener listener) throws JSONException {
       // update the exclusion dictionary
       JSONObject allow_notifications = this.getUserSettingJson("notifications");
       if (allow_notifications == null)
           allow_notifications = new JSONObject();

       JSONArray notify_list = allow_notifications.optJSONArray(evt_type);
       if (notify_list == null)
           notify_list = new JSONArray();

       int idx = TesUtils.findJsonArrayIndex(evt_cat, notify_list);
       if (idx < 0) {
           notify_list.put(evt_cat);
       }

       allow_notifications.put(evt_type, notify_list);

       // write the notification list to the server
       JSONObject params = new JSONObject();
       params.put("notifications", allow_notifications);
       final JSONObject allow_notifications_final = allow_notifications;
       this.updateAccountSettings(params, new TesApi.TesApiListener() {
           @Override
           public void onSuccess(JSONObject result) {
               TesWIApp.this.setUserSetting("notifications", allow_notifications_final);
               if (listener != null)
                   listener.onSuccess(result);
           }

           @Override
           public void onFailed(JSONObject result) {
               if (listener != null)
                   listener.onFailed(result);
           }

           @Override
           public void onOtherError(TesApiException error) {
               if (listener != null)
                   listener.onOtherError(error);
           }
       });
   }

   public void disallowNotifyEventType(String  evt_type, String evt_cat, final TesApi.TesApiListener listener) throws JSONException {
       // update the exclusion dictionary
       JSONObject allow_notifications = this.getUserSettingJson("notifications");
       if (allow_notifications == null){
           if (listener != null)
               listener.onOtherError(new TesApiException("No notifications found"));
           return;
       }

       JSONArray notify_list = allow_notifications.optJSONArray(evt_type);
       if (notify_list == null){
           if (listener != null)
               listener.onOtherError(new TesApiException("No notifications for event type found"));
           return;
       }


       boolean changed = false;
       int idx = TesUtils.findJsonArrayIndex(evt_cat, notify_list);
       if (idx >=0){
           notify_list.remove(idx);
           changed = true;
       }
       else {
           idx = TesUtils.findJsonArrayIndex("*",notify_list);
           if (idx >=0){
               notify_list.remove(idx);
               changed = true;
           }
       }

       if (changed){
           if (notify_list.length()<1){
                allow_notifications.remove(evt_type);
           }
            else {
                 allow_notifications.put(evt_type, notify_list);
           }

           // write the notification list to the server
           JSONObject params = new JSONObject();
           params.put("notifications", allow_notifications);
           final JSONObject allow_notifications_final = allow_notifications;
           this.updateAccountSettings(params, new TesApi.TesApiListener() {
               @Override
               public void onSuccess(JSONObject result) {
                   TesWIApp.this.setUserSetting("notifications", allow_notifications_final);
                   if (listener != null)
                       listener.onSuccess(result);
               }

               @Override
               public void onFailed(JSONObject result) {
                   if (listener != null)
                       listener.onFailed(result);
               }

               @Override
               public void onOtherError(TesApiException error) {
                   if (listener != null)
                       listener.onOtherError(error);
               }
           });
       }
       else {
           if (listener != null)
              listener.onOtherError(new TesApiException("No changes to exclusion list found"));
       }

   }

    public boolean isAllowNotify(String evt_type, String evt_cat){
        JSONObject allow_notifications = this.getUserSettingJson("notifications");
        if (allow_notifications == null)
            return false;

        JSONArray notify_list = null;
        try {
            notify_list = allow_notifications.getJSONArray(evt_type);
        } catch (JSONException e) {
            Log.i(TAG, String.format("Unable to get notify list: %s", e.toString()));
        }
        if (notify_list == null)
            return false;

        boolean allow = false;
        for (int i=0; i<notify_list.length(); i++){
            String cat = null;
            try {
                cat = notify_list.getString(i);
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get notify list item %d: %s", i, e.toString()));
            }
            if (cat.equals("*") || cat.equals(evt_cat)){
                allow = true;
                break;
            }
        }
        return allow;
    }

    // poi following support
    public boolean isFollowingPoi(String rid)
    {
        boolean fnd = false;
       JSONArray following = this.getUserSettingJsonArray("following");
        for (int i=0; i<following.length(); i++){
            JSONObject obj = null;
            try {
                obj = following.getJSONObject(i);
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get following list: %s", e.toString()));
            }

            String typ = null;
            String oid = null;
            try {
                typ = obj.getString("object_type");
                oid = obj.getString("object_id");
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get following list entry: %s", e.toString()));
            }

            if (typ.equals("poi") && oid.equals(rid)){
                fnd = true;
                break;
            }
        }
        return fnd;
    }

   public void addFollowPoi(String rid, final TesApi.TesApiListener listener) throws JSONException {
       if (this.isFollowingPoi(rid))
            return; // nothing to do.

       JSONArray following = this.getUserSettingJsonArray("following");
       if (following == null){
           following = new JSONArray();
       }

       JSONObject obj = new JSONObject();
       obj.put("object_type","poi");
       obj.put("object_id", rid);
       following.put(obj);

       JSONObject params = new JSONObject();
       params.put("following", following);

       final JSONArray following_final = following;
       this.updateAccountSettings(params, new TesApi.TesApiListener() {
           @Override
           public void onSuccess(JSONObject result) {
               TesWIApp.this.setUserSetting("following", following_final);
               if (listener != null)
                   listener.onSuccess(result);
           }

           @Override
           public void onFailed(JSONObject result) {
               if (listener != null)
                   listener.onFailed(result);
           }

           @Override
           public void onOtherError(TesApiException error) {
               if (listener != null)
                   listener.onOtherError(error);
           }
       });
   }

   public void removeFollowPoi(String rid, final TesApi.TesApiListener listener) throws JSONException{
       if (!this.isFollowingPoi(rid))
           return; // nothing to do.

       JSONArray following = this.getUserSettingJsonArray("following");
       if (following == null)
           return; // nothing to do


       for (int i=0; i<following.length(); i++){
           JSONObject obj = null;
           try {
               obj = following.getJSONObject(i);
           } catch (JSONException e) {
               Log.i(TAG, String.format("Unable to get list: %s", e.toString()));
           }

           String oid = null;
           String typ = null;
           try {
               typ = obj.getString("object_type");
               oid = obj.getString("object_id");
               if (oid.equals(rid) && typ.equals("poi")){
                   following.remove(i);
                   break;
               }
           } catch (JSONException e) {
               Log.i(TAG, String.format("Unable to get key list entry: %s", e.toString()));

           }
       }

       JSONObject params = new JSONObject();
       params.put("following", following);

       final JSONArray following_final = following;
       this.updateAccountSettings(params, new TesApi.TesApiListener() {
           @Override
           public void onSuccess(JSONObject result) {
               TesWIApp.this.setUserSetting("following", following_final);
               if (listener != null)
                   listener.onSuccess(result);
           }

           @Override
           public void onFailed(JSONObject result) {
               if (listener != null)
                   listener.onFailed(result);
           }

           @Override
           public void onOtherError(TesApiException error) {
               if (listener != null)
                   listener.onOtherError(error);
           }
       });
   }

   public void updateSettings(JSONObject params, TesApi.TesApiListener listener) throws JSONException {
        this.updateAccountSettings(params, listener);
   }

    // watch zones
    public JSONArray getWatchZones()
    {
        return this.getUserSettingJsonArray("watch_zones");
    }

    public JSONObject findZone(String name, JSONArray watchZones){
        JSONObject zone = null;
        for (int i=0; i<watchZones.length(); i++){
            JSONObject obj = null;
            try {
                obj = watchZones.getJSONObject(i);
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get zones list: %s", e.toString()));
            }

            String oid = null;
            try {
                oid = obj.getString("name");
                if (oid.equals(name)){
                    zone = obj;
                    break;
                }
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get zone list entry: %s", e.toString()));

            }
        }
        return zone;
    }

    public int findZoneIndex(String name, JSONArray watchZones){
        return TesUtils.findJsonArrayIndex("name", name, watchZones);
    }

    public JSONObject getWatchZoneNamed(String name){
        JSONArray watchZones = this.getUserSettingJsonArray("watch_zones");
        JSONObject zone = this.findZone(name, watchZones);
        return zone;
    }


    // adds a watch zone. If the there is a watchzone with the same name NO is returned otherwise yes is returned.

    public boolean addWatchZoneNamed(String  name,
                                     float distance,
                                     String  location,
                                     Location latLng,
                                     JSONObject oldZoneInfo,
                                     final TesApi.TesApiListener listener) throws JSONException
    {
        JSONArray watchZones = this.getUserSettingJsonArray("watch_zones");
        if (watchZones == null){
            watchZones = new JSONArray();
        }

        // remove the old version of the zone
        if (oldZoneInfo != null){
            String oldName = null;

            oldName = oldZoneInfo.getString("name");
            JSONObject zone = this.findZone(oldName,watchZones);
            if (zone != null) {
                for (int i = 0; i < watchZones.length(); i++) {
                    if (watchZones.getJSONObject(i).getString("name").equals(oldName)) {
                        watchZones.remove(i);
                        break;
                    }
                }
            }

        }

        // see if we have a duplicate name
        JSONObject zone = this.findZone(name,watchZones);
        if (zone != null){
            return false;
        }

        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("distance", distance);

        if (latLng == null){
            obj.put("current_location", true);
        }
        else
        {
            obj.put("current_location", true);
            obj.put("location", latLng);
        }
        watchZones.put(obj);

        JSONObject params = new JSONObject();
        params.put("watch_zones", watchZones);

        final JSONArray finalWatchZones = watchZones;
        this.updateSettings(params, new TesApi.TesApiListener() {
            @Override
            public void onSuccess(JSONObject result) {
                TesWIApp.this.setUserSetting("watch_zones", finalWatchZones);

                if (listener != null)
                    listener.onSuccess(result);
            }

            @Override
            public void onFailed(JSONObject result) {
                if (listener != null)
                    listener.onFailed(result);
            }

            @Override
            public void onOtherError(TesApiException error) {
                if (listener != null)
                    listener.onOtherError(error);

            }
        });

        return true;
    }

   public void removeWatchZonenamed(String name , final TesApi.TesApiListener listener) throws JSONException {

       final JSONArray watchZones = this.getUserSettingJsonArray("watch_zones");
       if (watchZones == null)
           return; // nothing to do

       int zoneIdx = this.findZoneIndex(name, watchZones);
       if (zoneIdx < 0)
           return; // nothing to do

       watchZones.remove(zoneIdx);

       JSONObject params = new JSONObject();
       params.put("watch_zones", watchZones);

       this.updateSettings(params, new TesApi.TesApiListener() {
           @Override
           public void onSuccess(JSONObject result) {
               TesWIApp.this.setUserSetting("watch_zones", watchZones);

               if (listener != null)
                   listener.onSuccess(result);
           }

           @Override
           public void onFailed(JSONObject result) {
               if (listener != null)
                   listener.onFailed(result);
           }

           @Override
           public void onOtherError(TesApiException error) {
               if (listener != null)
                   listener.onOtherError(error);

           }
       });
   }



}

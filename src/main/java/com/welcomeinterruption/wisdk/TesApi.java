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
import android.graphics.Bitmap;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;


public class TesApi {
    /**
     response status
     */
    public final static int TESCallSuccessOK = 0;     // call succeeded with an OK status
    public final static int TESCallSuccessFAIL = 1;   // call succeeded but the json packet returned failure
    public final static int TESCallError = 2;
    /**
     * status of call if in error
     */
    public final static int TESResultGood = 0;         // no error in the result.  All good
    public final static int TESResultErrorNetwork = 1;  // general nework error
    public final static int TESResultErrorHttp = 2;     // http server returned a bad status
    public final static int TESResultErrorAuth = 3;     // either a 401 or 403 was returned

    /**
     * Auth types
     */
    public final static String TES_AUTH_TYPE_NATIVE = "native";
    public final static String  TES_AUTH_TYPE_FACEBOOK = "facebook";
    public final static String  TES_AUTH_TYPE_GOOGLE  = "google";
    public final static String  TES_AUTH_TYPE_ANONYMOUS = "anonymous";


    public interface TesApiListener {
         void onSuccess(JSONObject result);
         void onFailed(JSONObject result);
         void onOtherError(Exception error);
    }

    public interface TesApiAuthListener {
        /**
         sent what an authorization has failed
         */
        void httpAuthFailure(NetworkResponse response);

    }

    private  Context mCtx;
    private  String mEndpoint;

    /**
     Authentication token to be passed to all endpoints that require an authenticated user.
     this token is returned from a login call
     */


    public String accessToken;
    public String accessAuthType;
    public JSONObject accessAuthInfo;
    public JSONObject accessSystemDefaults;

    public Map<String, String> headers;

    // TODO: get rid of this section as it should all be in wiapp
    private  String push_info = null;
    private  String push_type = null;
    private  String user_locale = null;
    private  int user_timezone_offset = -1;

    public @Nullable TesApiAuthListener authListener = null;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    public TesApi(Context context) {
        this.mCtx = context;
        this.mRequestQueue = getRequestQueue();
        this.headers = Collections.emptyMap();

        this.mImageLoader = new ImageLoader(this.mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }


    public String getEndpoint() {
        return this.mEndpoint;
    }

    public void setEndpoint(String mEndpoint) {
        this.mEndpoint = mEndpoint;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String newtoken) {
         this.accessToken = newtoken;
    }

    /**
     checks to see if a web reguest is successful. The default implementation  checks the response
     dictionary for a boolean field called success

     @param result The dictionary returned from the request
     **/
    public boolean isSuccessful(@NonNull JSONObject result){
        try {
            return result.getBoolean("success");
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     returns whether the user has an authorziation token or not
     */

    public boolean isAuthorized()
    {
        return this.accessToken != null;
    }

    /**
     Check for authorization failure by interpreting the NSError returned

     @param error - the error object retunred
     **/
    public boolean isAuthFailure(@NonNull TesApiException error)
    {
        if (!(error.getCause() instanceof VolleyError))
             return false;

        VolleyError verr = (VolleyError) error.getCause();
        return this.isAuthFailure(verr);
    }

    public boolean isAuthFailure(@NonNull VolleyError error){
        boolean authFailure = false;
        if (error instanceof AuthFailureError || error instanceof NoConnectionError){
            if (error.networkResponse != null && (error.networkResponse.statusCode == 403 || error.networkResponse.statusCode == 401)){
                authFailure = true;
            }
        }
        return authFailure;
    }

    // TODO : get rid of this section - it should all be in wiapp
    public String getPushInfo() {
        return this.push_info;
    }

    public void setPushInfo(String push_info) {
        this.push_info = push_info;
    }

    public String getPushType() {
        return push_type;
    }

    public void setPushType(String push_type) {
        this.push_type = push_type;
    }

    public RequestQueue getRequestQueue() {
        if (this.mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            this.mRequestQueue = Volley.newRequestQueue(this.mCtx.getApplicationContext());
        }
        return this.mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return this.mImageLoader;
    }

    public void login(String username, String password, TesApiListener listener){

        JSONObject body = new JSONObject();
        try {
            body.put("user_name", username);
            body.put("password", password);
        }
        catch (JSONException e){
            if (listener != null)
                listener.onOtherError(new TesApiException(e.getLocalizedMessage(), e));
        }
        TesApiListener authListener =  wrapAuthListener(listener);
        call(Request.Method.POST, "/auth/login", body, authListener, false);
    }

    public void register(String email,
                         String password,
                         String firstName,
                         String lastName,
                         boolean anonymous,
                         String programName,
                         String programExternalId,
                         TesApiListener listener){
        JSONObject body = new JSONObject();
        try {
            if (email != null) body.put("email", email);
            if (password != null) body.put("password", password);
            if (firstName != null) body.put("firstName", firstName);
            if (lastName != null) body.put("lastName", lastName);
            if (anonymous) body.put("anonymous", anonymous);
            if (programName != null){
                JSONObject attrs = new JSONObject();
                attrs.put("name", programName);
                if (programExternalId != null) attrs.put("external_id", programExternalId);
                body.put("program_attr", attrs);
            }
        }
        catch (JSONException e){
            if (listener != null)
                listener.onOtherError(new TesApiException(e.getLocalizedMessage(), e));
        }
        TesApiListener authListener = wrapAuthListener(listener);
        call(Request.Method.POST, "/account", body, authListener, false);

    }

    public void profile(TesApiListener listener){
        call(Request.Method.GET, "/account", null, listener, true);
    }

    public void geodevice(Location location, boolean background, TesApiListener listener){
        JSONObject body = new JSONObject();
        try {
            float accuracy = location.getAccuracy();
            float speed = location.getSpeed();
            float bearing = location.getBearing();

            JSONObject current = new JSONObject();
            JSONArray coordinates = new JSONArray();

            coordinates.put(location.getLongitude());
            coordinates.put(location.getLatitude());

            current.put("accuracy", accuracy);
            current.put("speed", speed);
            current.put("coordinates", coordinates);
            current.put("course", bearing);
            current.put("in_background", background);

            body.put("current", current);
            if (this.push_info != null && this.push_info.length()>0)
                body.put("push_info", this.push_info);
            if (this.push_type != null && push_type.length()>0)
                body.put("push_type", this.push_type);
            if (this.user_locale != null && user_locale.length()>0)
                body.put("locale", user_locale);
            if (this.user_timezone_offset != -1)
                body.put("timezone_offset", this.user_timezone_offset);

        }
        catch (JSONException e){
            if (listener != null)
                listener.onOtherError(new TesApiException(e.getLocalizedMessage(), e));
        }
        call(Request.Method.POST, "/geodevice", body, listener, true);
    }

    public void call(Integer method, String url, JSONObject body, final TesApiListener listener, Boolean authenticate) {

        StringBuilder endpoint = new StringBuilder();
        endpoint.append(this.mEndpoint);
        if (!url.startsWith("/"))
            endpoint.append("/");
        endpoint.append(url);
        if (authenticate && this.accessToken != null){
            endpoint.append((endpoint.indexOf("?")>=0) ? "&" : "?");
            endpoint.append("token=");
            endpoint.append(this.accessToken);
        }

        final  String finalUrl = endpoint.toString();
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (method, finalUrl, body, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Boolean success;
                        try {
                            success = response.getBoolean("success");
                        }
                        catch (JSONException e){
                            success = false;
                        }
                        if (listener != null){
                            if (success)
                                listener.onSuccess(response);
                            else
                                listener.onFailed(response);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (TesApi.this.isAuthFailure(error)){
                            if (TesApi.this.authListener != null)
                                TesApi.this.authListener.httpAuthFailure(error.networkResponse);
                        }
                        if (listener != null)
                           listener.onOtherError(new TesApiException("Network Error", error));

                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return Collections.emptyMap();
            }
        };
        addToRequestQueue(jsObjRequest);
    }

    private TesApiListener wrapAuthListener(final TesApiListener inner){
        return new TesApiListener() {
            @Override
            public void onSuccess(JSONObject result){
                try {
                    String token = result.getJSONObject("data").getString("token");
                    setAccessToken(token);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                inner.onSuccess(result);
            }

            @Override
            public void onFailed(JSONObject result) {
                inner.onFailed(result);
            }

            @Override
            public void onOtherError(Exception error) {
                inner.onOtherError(error);
            }

        };
    }


}

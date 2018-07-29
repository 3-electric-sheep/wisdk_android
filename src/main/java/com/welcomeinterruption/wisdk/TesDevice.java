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

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TesDevice {
    public @Nullable String pushToken = null;
    public @Nullable String pushType = null;
    public @Nullable String pushProfile = null;
    public @Nullable String locale = null;
    public @Nullable String timezoneOffset = null;
    public @Nullable String version = null;

    public @NonNull TesLocationInfo current;
    public @Nullable JSONArray pushTargets = null;

    public TesDevice(@NonNull JSONObject attributes) throws JSONException {

        this.pushToken = attributes.optString("push_info", this.pushToken);
        this.pushType = attributes.optString("push_type", this.pushType);
        this.pushProfile = attributes.optString("push_profile", this.pushProfile);

        JSONObject currloc = attributes.optJSONObject("current");
        if (currloc != null)
            this.current = new TesLocationInfo(currloc);

        this.timezoneOffset = attributes.optString("timezone_offset", this.timezoneOffset);
        this.locale = attributes.optString("locale", this.locale);
        this.version = attributes.optString("version", this.version);

        JSONArray pushtgts = attributes.optJSONArray("push_targets");
        if (pushtgts != null)
            this.pushTargets = pushtgts;
    }

    public @NonNull JSONObject toDictionary() throws JSONException {
        JSONObject res = new JSONObject();
        res.putOpt("push_info", this.pushToken);
        res.putOpt("push_type", this.pushType);
        res.putOpt("push_profile", this.pushProfile);
        res.putOpt("current", this.current.toDictionary());
        res.putOpt("timezone_offset", this.timezoneOffset);
        res.putOpt("locale", this.locale);
        res.putOpt("version", this.version);
        res.putOpt("push_targets", this.pushTargets);
        return res;
    }
}

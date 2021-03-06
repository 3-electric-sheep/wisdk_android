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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class TesPushInfo {
    public @Nullable String pushType;
    public @Nullable String pushToken;
    public @Nullable String pushProfile;

    public TesPushInfo(@Nullable String pushType, @Nullable String pushToken, @Nullable String pushProfile) {
        this.pushType = pushType;
        this.pushToken = pushToken;
        this.pushProfile = pushProfile;
    }

    public TesPushInfo(@NonNull JSONObject attributes)
    {
        this.pushToken = attributes.optString("push_info");
        this.pushType = attributes.optString("push_type");
        this.pushProfile = attributes.optString("push_profile");
    }


    JSONObject toDictionary() throws JSONException
    {
        JSONObject res = new JSONObject();
        if (this.pushToken != null)
            res.put("push_info", this.pushToken);

        if (this.pushType != null)
            res.put("push_type", this.pushType);

        if (this.pushProfile != null)
            res.put("push_profile", this.pushProfile);

        return res;
    }
}

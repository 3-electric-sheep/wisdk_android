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

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class TesDictionary extends LinkedHashMap<String, Object> {

    private final String TAG = "TesDictionary";

    public TesDictionary(JSONObject json) throws JSONException {
        super();
        this._toDict(json);
    }

    public TesDictionary() {
        super();
    }

    public JSONObject toJson(){
        try {
            return new JSONObject(this);
        }
        catch (NullPointerException e){
            Log.e(TAG, "Null pointer exception while converting dictionary");
            return null;
        }
    }
    
    public void fromJson(String jsonString) throws JSONException{
        JSONObject json = new JSONObject(jsonString);
        this._toDict(json);
    }

    private void _toDict(JSONObject json) throws  JSONException {
        Iterator<?> keys = json.keys();
        while (keys.hasNext()){
            String key = (String)keys.next();
            this.put(key, json.get(key));
        }
    }
}

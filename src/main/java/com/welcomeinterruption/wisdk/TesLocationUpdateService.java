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


import org.json.JSONObject;

import com.firebase.jobdispatcher.JobParameters;


public class TesLocationUpdateService extends TesJobService {
    private static final String TAG = TesLocationUpdateService.class.getSimpleName();

    @Override
    public boolean doWork(final JobParameters params, JSONObject jsArgs) {
        TesWIApp wi = TesWIApp.manager();
        wi.sendLocationUpdate(jsArgs, new TesApi.TesApiListener() {
            @Override
            public void onSuccess(JSONObject result) {
                TesLocationUpdateService.this.jobFinished(params, false);
            }

            @Override
            public void onFailed(JSONObject result) {
                TesLocationUpdateService.this.jobFinished(params, false);
            }

            @Override
            public void onOtherError(Exception error) {
                TesLocationUpdateService.this.jobFinished(params, false);
            }
        });
        return true;
    }

};


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

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolvableVoidResult;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CreateWalletObjectsRequest;
import com.google.android.gms.wallet.OfferWalletObject;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.WalletObjectsClient;
import com.google.android.gms.wallet.wobs.TimeInterval;
import com.google.android.gms.wallet.wobs.WalletObjectsConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by pfrantz on 11/08/2017.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */

public class TesWalletMgr
       implements GoogleApiClient.OnConnectionFailedListener {

        private static final String TAG = "TesWalletMgr";
        private static final int SAVE_TO_ANDROID = 888;

        public static final String WALLET_NOTIFICATION = "WalletNotify";

        private String SUCCESS_RESPONSE_TEXT = "saved";
        private String CANCELED_RESPONSE_TEXT = "canceled";
        private String ERROR_PREFIX_TEXT = "failed error code: ";

        public static final Scope WOB =
                new Scope("https://www.googleapis.com/auth/wallet_object.issuer");

        private WalletObjectsClient walletObjectsClient;
        private TesWIApp.TesWIAppListener listener;

        protected void init(TesWIApp.TesWIAppListener listener) {
           this.listener = listener;
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.w(TAG, "onConnectionFailed: " + result);
        }

    public void saveToAndroid(Activity activity, JSONObject walletObj) {
        OfferWalletObject wob = generateOfferWalletObject(walletObj);
        CreateWalletObjectsRequest request = new CreateWalletObjectsRequest(wob);
        Wallet.WalletOptions walletOptions = new Wallet.WalletOptions.Builder()
                .setTheme(WalletConstants.THEME_LIGHT)
                .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
                .build();

        walletObjectsClient = Wallet.getWalletObjectsClient(activity, walletOptions);
        Task<AutoResolvableVoidResult> task = walletObjectsClient.createWalletObjects(request);
        AutoResolveHelper.resolveTask(task, activity, SAVE_TO_ANDROID);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String msg = "";
        switch (requestCode) {
            case SAVE_TO_ANDROID:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                         msg = SUCCESS_RESPONSE_TEXT;
                        break;
                    case Activity.RESULT_CANCELED:
                        msg = CANCELED_RESPONSE_TEXT;
                        break;
                    default:
                        int errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
                        msg = ERROR_PREFIX_TEXT + errorCode;
                        break;
                }
        }
        if (this.listener != null)
            this.listener.saveWallet(requestCode, resultCode, data, msg);

    }

    public OfferWalletObject generateOfferWalletObject(JSONObject walletObj) {

        OfferWalletObject wob = null;
        try {
            String kind = walletObj.getString("kind");

            String classId = walletObj.getString("classId");
            String objectId = walletObj.getString("id");
            String state = walletObj.getString("state");

            if (!kind.equalsIgnoreCase("walletobjects#offerObject")){
                Log.e(TAG, "Invalid wallet offer kind: "+kind);
                return null;
            }

            TimeInterval timeInterval = null;
            JSONObject timeObj = walletObj.optJSONObject("validTimeInterval");
            if (timeObj != null) {
                Date startDate = TesISO8601DateParser.parse(timeObj.getJSONObject("start").getString("date"));
                Date endDate = TesISO8601DateParser.parse(timeObj.getJSONObject("end").getString("date"));
                timeInterval = new TimeInterval(startDate.getTime(), endDate.getTime());
            }

            JSONObject barcode = walletObj.optJSONObject("barcode");
            String barcodeType = null;
            String barcodeValue = null;
            String barcodeLabel = null;
            if (barcode != null) {
                barcodeType = barcode.getString("type");
                barcodeValue = barcode.getString("value");
                barcodeLabel = barcode.optString("label");
            }

            OfferWalletObject.Builder builder = OfferWalletObject.newBuilder()
                    .setId(objectId)
                    .setClassId(classId)
                    .setState((state.equalsIgnoreCase("active")) ? WalletObjectsConstants.State.ACTIVE : WalletObjectsConstants.State.INACTIVE);

            if (timeInterval != null)
                builder.setValidTimeInterval(timeInterval);

            if (barcodeType != null) {
                builder.setBarcodeLabel(barcodeLabel)
                   .setBarcodeType(barcodeType)
                   .setBarcodeValue(barcodeValue);
            }

            wob = builder.build();

        }
        catch (JSONException e){
            Log.e(TAG, "Failed to load wallet object:"+e.getLocalizedMessage());
            e.printStackTrace();
            return null;
        } catch (ParseException e) {
            Log.e(TAG, "Failed to load wallet object. Invalid date:"+e.getLocalizedMessage());
            e.printStackTrace();
        }

        return wob;
    }
}

/*******************************************************************************
 * Copyright 2015 eTilbudsavis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.eTilbudsavis.sdkdemo;

import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import com.eTilbudsavis.etasdk.Eta;
import com.eTilbudsavis.etasdk.EtaLocation;
import com.eTilbudsavis.etasdk.log.DevLogger;
import com.eTilbudsavis.etasdk.log.EtaLog;
import com.eTilbudsavis.etasdk.test.EtaSdkTest;

public class MainPreferenceActivity extends BasePreferenceActivity implements Preference.OnPreferenceClickListener {

    public static final String TAG = MainPreferenceActivity.class.getSimpleName();

    public static final String KEY_SDK_UNIT_TEST = "pref_main_sdk_unit_test";

    Runnable mSDKUnitTest = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainPreferenceActivity.this, "Running SDK Unit Test", Toast.LENGTH_SHORT).show();
            EtaSdkTest.test();
            Toast.makeText(MainPreferenceActivity.this, "SDK Unit Test: Success", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EtaLog.setLogger(new DevLogger());

        /*
         * Eta is a singleton you interact with via this method
         */
        Eta eta = Eta.getInstance();

        /*
         * Set the location (This could also be set via LocationManager)
         */
        EtaLocation loc = eta.getLocation();
        loc.setLatitude(Constants.ETA_HQ.lat);
        loc.setLongitude(Constants.ETA_HQ.lng);

        // Avoid using large distances in production, it's bad for performance (longer queries)
        // the 700km radius here is just for demonstration purposes - we recommend 100km or less
        loc.setRadius(700000);
        loc.setSensor(false);

        EtaLog.d(TAG, loc.toString());

        /*
         * You are now done setting up the SDK, the rest is just Android stuff
         */
        addPreferencesFromResource(R.xml.main_preference_activity_layout);
        findPreference(KEY_SDK_UNIT_TEST).setOnPreferenceClickListener(this);

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (KEY_SDK_UNIT_TEST.equals(preference.getKey())) {
            new Thread(mSDKUnitTest).run();
            return true;
        }
        return false;
    }
}

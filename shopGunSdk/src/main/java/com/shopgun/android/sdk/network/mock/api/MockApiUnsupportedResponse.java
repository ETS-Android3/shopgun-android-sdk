/*******************************************************************************
 * Copyright 2015 ShopGun
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

package com.shopgun.android.sdk.network.mock.api;

import android.content.Context;

import com.shopgun.android.sdk.network.NetworkResponse;
import com.shopgun.android.sdk.network.Request;
import com.shopgun.android.sdk.network.mock.MockUnsupportedNetworkResponse;

public class MockApiUnsupportedResponse extends MockApiNetworkResponse {

    protected MockApiUnsupportedResponse(Context mContext, Request<?> request) {
        super(mContext, request);
    }

    @Override
    public NetworkResponse getResponse() {
        return new MockUnsupportedNetworkResponse(mRequest);
    }

}

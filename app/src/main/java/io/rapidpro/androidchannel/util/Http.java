/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.androidchannel.util;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


import java.util.HashMap;
import java.util.Map;

import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.json.JSON;

/**
 * HttpWrapper wraps all http interaction to allow for intelligent
 * caching and shared client settings.
 *
 * All HTTP calls pass through this class, which makes sure that caching is done
 * intelligently.
 */
public class Http {
    private static final int TIMEOUT = 120000;
    private static final String USER_AGENT = "RapidPro/" + Build.VERSION.RELEASE;

    public static String contents;

    public Http() {
    }


    public JSON fetchJSON(String url, String postData) throws JSONException {

        RequestQueue requestQueue = Volley.newRequestQueue(RapidPro.get());

        ResponseListener listener = new ResponseListener() {
            @Override
            public void onResponse(JSONObject obj) {
                contents = obj.toString();
            }
        };

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(postData), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        listener.onResponse(response);
                        RapidPro.LOG.d("/n/n    " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        RapidPro.LOG.e("Error getting response", error);

                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("User-Agent", USER_AGENT);

                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy(
                new DefaultRetryPolicy(
                        TIMEOUT,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(jsonObjectRequest);

        return new JSON(new String(contents));

    }

    public interface ResponseListener{
        public void onResponse(JSONObject obj);
    }
}

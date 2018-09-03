/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.quakereport;

import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods related to requesting and receiving earthquake data from USGS.
 */
public final class QueryUtils {

    /** Tag for the log messages */
    private static final String LOG_TAG = QueryUtils.class.getSimpleName();

    /**
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    /**
     * Query the USGS dataset and return a list of {@link Earthquake} objects.
     */

    public static List<Earthquake> fetchEarthquakeData(String requestUrl){

        Log.i(LOG_TAG,"TEST:fetchEarthquakeData() called...");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //create URL object
        URL url = createUrl(requestUrl);

        //perform HTTP request to the URL and recive a JSON response back;
        String jsonResponse = null;
        try{
            jsonResponse = makeHttpRequest(url);
        }catch (IOException e){
            Log.e(LOG_TAG,"Problem making the HTTP request ",e);
        }

        List<Earthquake> earthquakes = extractFeatureFromJson(jsonResponse);

        // Return the list of {@link Earthquake}s
        return earthquakes;
    }

    private static URL createUrl(String stringUrl){
        URL url = null;
        try {
            url = new URL(stringUrl);
        }catch (MalformedURLException e){
            Log.e(LOG_TAG,"Problem building URL object ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String JSON as the response.
     */
    private static String makeHttpRequest(URL url) throws  IOException{
        String jsonResponse = "";

        if(url == null){
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try{
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if(urlConnection.getResponseCode() == 200){
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            }else{
                Log.e(LOG_TAG,"Error response code : " + urlConnection.getResponseCode());
            }
        }catch (IOException e){
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        }finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            if(inputStream != null){
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private static String readFromStream(InputStream inputStream) throws  IOException{
        StringBuilder output = new StringBuilder();

        if(inputStream != null){
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null){
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return a list of {@link Earthquake} objects that has been built up from
     * parsing the given JSON response.
     */

        private static List<Earthquake> extractFeatureFromJson (String earthquakeJSON){
            //if the JSON is empty or null,then return early
            if(TextUtils.isEmpty(earthquakeJSON)){
                return null;
            }

            List<Earthquake> earthquakes = new ArrayList<>();

           try {
               // Create a JSONObject from the JSON response string
               JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);
               JSONArray earthquakeArray = baseJsonResponse.getJSONArray("features");
               // For each earthquake in the earthquakeArray, create an {@link Earthquake} object
               for(int i = 0 ; i < earthquakeArray.length(); i++){
                   // Get a single earthquake at position i within the list of earthquakes
                   JSONObject currentEarthquake = earthquakeArray.getJSONObject(i);
                   JSONObject properties = currentEarthquake.getJSONObject("properties");

                   // Extract the value for the key called "mag"
                   double magnitude = properties.getDouble("mag");

                   // Extract the value for the key called "place"
                   String location = properties.getString("place");

                   // Extract the value for the key called "time"
                   long time = properties.getLong("time");

                   // Extract the value for the key called "url"
                   String url = properties.getString("url");

                   // Create a new {@link Earthquake} object with the magnitude, location, time,
                   // and url from the JSON response.
                   Earthquake earthquake = new Earthquake(magnitude, location, time, url);

                   // Add the new {@link Earthquake} to the list of earthquakes.
                   earthquakes.add(earthquake);
               }
           }catch (JSONException e){
               Log.e(LOG_TAG,"JSON extract data failed!");
           }

           return earthquakes;
        }
}

package jacob.webrequests;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jacob on 10/24/17.
 */

public class WebRequest {
    public void makeRequest(RequestQueue queue, String url) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
//                        mTextView.setText("Response is: "+ response.substring(0,500));
                        Log.v("Response", response.substring(0, 500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                mTextView.setText("That didn't work!");
                Log.v("Error", error.toString());
            }
        });

        queue.add(stringRequest);
    }

    public void streetcarJsonRequest(RequestQueue queue, final String url, final OnTaskCompleted callback) {
        JsonArrayRequest jsArrayRequest = new JsonArrayRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {
                    Log.v("Response", response.toString());
                    callback.onTaskCompleted(response);
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v("Error", "Error fetching " + url + error.toString());

                }
            });
        queue.add(jsArrayRequest);
    }

    public void stopsRoutesJsonRequest(RequestQueue queue, final String url, final OnTaskCompleted callback) {
        JsonObjectRequest jsObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject route = response.getJSONObject("route");

                            JSONArray path = route.getJSONArray("path");
                            JSONArray stops = route.getJSONArray("stop");


                            Log.v("Response", stops.toString());
                            callback.onTaskCompleted(stops);
                        }
                        catch (JSONException error) {
                            Log.v("Error", "There was an error converting Object to array " + error.toString());

                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("Error", "Error fetching " + url + error.toString());

                    }
                });
        queue.add(jsObjectRequest);
    }
}

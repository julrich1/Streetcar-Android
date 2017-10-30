package jacob.SeattleStreetcarTracker;

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

import java.util.ArrayList;

/**
 * Created by jacob on 10/24/17.
 */

public class WebRequest {
    public static final String REQUEST_TAG = "SCRequests";


    public void streetcarJsonRequest(RequestQueue queue, final String url, final FetchStreetcars callback) {
        JsonArrayRequest jsArrayRequest = new JsonArrayRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {
//                    Log.v("Response", response.toString());
                    callback.onTaskCompleted(response);
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v("Error", "Error fetching " + url + error.toString());

                }
            });
        jsArrayRequest.setTag(REQUEST_TAG);
        queue.add(jsArrayRequest);
    }

    public void stopsRoutesJsonRequest(RequestQueue queue, final String url, final FetchedStopsAndRoutes callback) {
        JsonObjectRequest jsObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject route = response.getJSONObject("route");

                            JSONArray paths = route.getJSONArray("path");
                            JSONArray stops = route.getJSONArray("stop");

                            callback.onTaskCompleted(stops, paths);
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
        jsObjectRequest.setTag(REQUEST_TAG);
        queue.add(jsObjectRequest);
    }

    public void getArrivalTimes(RequestQueue queue, final String url, final FetchArrivalTimes callback) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList arrivalTimes = new ArrayList();

                        Log.v("Response", response.toString());
                        try {
                            String stopName = response.getJSONObject("predictions").optString("stopTitle");
                            arrivalTimes.add(stopName);

                            int stopId = response.getJSONObject("predictions").optInt("stopTag");
                            arrivalTimes.add(stopId);


                            JSONArray predictionsArray = response.getJSONObject("predictions").getJSONObject("direction").getJSONArray("prediction");

                            for (int i = 0; i < predictionsArray.length(); i++) {
                                int arrivalTime = predictionsArray.getJSONObject(i).optInt("minutes");
                                arrivalTimes.add(arrivalTime);
                            }

                            callback.onTaskCompleted(arrivalTimes);
                        }
                        catch (JSONException error) {
                            Log.v("Error", "There was an error parsing JSON " + error.toString());
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("Error", "Error fetching " + url + error.toString());

                    }
                });
        jsonObjectRequest.setTag(REQUEST_TAG);
        queue.add(jsonObjectRequest);
    }

    public void getMultipleArrivalTimes(RequestQueue queue, final String url, final FetchArrivalTimes callback) {


    }

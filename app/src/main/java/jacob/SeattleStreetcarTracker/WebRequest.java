package jacob.SeattleStreetcarTracker;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jacob on 10/24/17.
 */

public class WebRequest {
    public static final String REQUEST_TAG = "SCRequests";
    public static final String ARRIVAL_REQUEST_TAG = "SCArrivals";


    public void streetcarJsonRequest(RequestQueue queue, final String url, final FetchStreetcars callback) {
        JsonArrayRequest jsArrayRequest = new JsonArrayRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {
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
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        callback.onTaskCompleted(parseArrivalObject(response));
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("Error", "Error fetching " + url + error.toString());

                    }
                });

        jsonArrayRequest.setTag(ARRIVAL_REQUEST_TAG);
        queue.add(jsonArrayRequest);
    }

    private ArrayList parseArrivalObject(JSONArray response) {
        ArrayList arrivalTimes = new ArrayList();

        try {
            JSONObject stopDetail = response.getJSONObject(0);
            String stopName = stopDetail.getString("stopTitle");
            arrivalTimes.add(stopName);

            int stopId = stopDetail.getInt("stopId");
            arrivalTimes.add(stopId);

            JSONArray predictionsArray = stopDetail.getJSONArray("arrivals");

            for (int i = 0; i < predictionsArray.length(); i++) {
                String arrivalTime = predictionsArray.optString(i);
                arrivalTimes.add(arrivalTime);
            }
        }
        catch (JSONException error) {
            Log.v("Error", "There was an error parsing JSON " + error.toString());
        }

        return arrivalTimes;
    }

    public void getMultipleFavoriteArrivalTimes(RequestQueue queue, final String url, final FetchAllArrivalTimes callback) {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        ArrayList<ArrayList> arrivals = new ArrayList<>();
                        ArrayList arrival;

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                arrival = new ArrayList();

                                JSONObject prediction = response.getJSONObject(i);
                                arrival.add(prediction.optInt("stopTitle"));

                                JSONArray predictionArray = prediction.getJSONArray("arrivals");

                                for (int j = 0; j < predictionArray.length(); j++) {
                                    arrival.add(predictionArray.optInt(j));
                                }

                                arrivals.add(arrival);
                            }

                            callback.onTaskCompleted(arrivals);
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

        jsonArrayRequest.setTag(REQUEST_TAG);
        queue.add(jsonArrayRequest);
        }
    }

package jacob.SeattleStreetcarTracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    String API_URL = "http://sc-dev.shadowline.net";

    private GoogleMap mMap;
    public Streetcars streetcars = new Streetcars();
    Timer scTimer = new Timer();

    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout_new);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        queue = Volley.newRequestQueue(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.v("Event pause", "onPause() was called");
        scTimer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.v("Event resume", "onResume() was called");

        scTimer = new Timer();
        startTimers();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        SlidingUpPanelLayout panel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        panel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.v("Click event", "Triggered");

        if (marker.getTag() != null) {
            Log.v("Marker tag is :", marker.getTag().toString());
            getArrivalTime((int) marker.getTag());
            SlidingUpPanelLayout panel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
            panel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        getStops();
        initializeStreetcars();
    }

    private void initializeStreetcars() {
        getStreetcars(new Callback() {
            @Override
            public void done() {
                for (int i = 0; i < streetcars.length(); i++) {
                    createMarker(streetcars.get(i));
                }
            }
        });
    }

    private void startTimers() {
        Log.v("Event startTimers", "startTimers() was called");

        scTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                updateStreetcars();
            }
        }, 0, 2000);
    }

    private void getStreetcars(final Callback cb) {
        String url = API_URL + "/api/streetcars/1";

        WebRequest wr = new WebRequest();
        wr.streetcarJsonRequest(queue, url, new FetchStreetcars() {
            @Override
            public void onTaskCompleted(JSONArray response) {
                Gson gson = new Gson();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        Streetcar streetcar = gson.fromJson(response.getJSONObject(i).toString(), Streetcar.class);
                        streetcars.update(streetcar);
                    }
                    catch (JSONException error) {
                        Log.v("Error", "Error getting JSON Object");
                    }
                }
//                updateStreetcars();
                cb.done();
            }
        });
    }

    private void getStops() {
        String url = API_URL + "/api/routes/1";

        final ArrayList<Stop> stops = new ArrayList<>();

        WebRequest wr = new WebRequest();
        wr.stopsRoutesJsonRequest(queue, url, new FetchedStopsAndRoutes() {
            @Override
            public void onTaskCompleted(JSONArray stopsArray, JSONArray paths) {
                Gson gson = new Gson();

                for (int i = 0; i < stopsArray.length(); i++) {
                    try {
                        Log.v("Stop response", stopsArray.toString());
                        Stop stop = gson.fromJson(stopsArray.getJSONObject(i).toString(), Stop.class);
                        stops.add(stop);
                    }
                    catch (JSONException error) {
                        Log.v("Error", "Error getting JSON Object");
                    }
                }

                for (int i = 0; i < paths.length(); i++) {
                    try {
                        JSONObject obj = paths.getJSONObject(i);
                        JSONArray currentPath = obj.getJSONArray("point");
                        ArrayList<LatLng> points = new ArrayList<>();

                        for (int j = 0; j < currentPath.length(); j++) {
                            points.add(new LatLng(currentPath.getJSONObject(j).optDouble("lat"), currentPath.getJSONObject(j).optDouble("lon")));
                        }

                        drawRouteLines(points);
                    }
                    catch(JSONException error) {
                        Log.v("Error", "Error getting path");
                    }
                }

                drawStops(stops);
            }
        });
    }

    private MarkerOptions setMarkerOptions(Streetcar streetcar) {
        LatLng location = new LatLng(streetcar.x, streetcar.y);

        return new MarkerOptions()
            .position(location)
            .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("streetcar",150,150)))
            .title("Info")
            .anchor(0.5f, 0.5f)
            .rotation(streetcar.heading)
            .zIndex(1.0f);
    }

    private void createMarker(Streetcar streetcar) {
        streetcar.marker = mMap.addMarker(setMarkerOptions(streetcar));
        Log.v("Create marker:", "marker is: " + streetcar.marker.toString());
//            marker.setTag(streetcars.get(i).streetcar_id);
    }

    public void updateStreetcars() {
        getStreetcars(new Callback() {
            @Override
            public void done() {
                Streetcar streetcar;

                for(int i = 0; i < streetcars.length(); i++) {
                    Log.v("updating: ", "Streetcar:" + i);
                    streetcar = streetcars.get(i);
                    if (streetcar.marker == null) {
                        Log.v("Error: ", "Marker not found!");
                    }
                    else {
                        streetcar.marker.setRotation(streetcar.heading);
                        streetcar.marker.setSnippet("Location: " + streetcar.x + " " + streetcar.y + "Last Speed " + convertKmHrToMph(streetcar.speedkmhr));
                        LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Spherical();
                        MarkerAnimation.animateMarkerToICS(streetcar.marker, new LatLng(streetcar.x, streetcar.y), latLngInterpolator);
//                        streetcar.marker.setPosition(new LatLng(streetcar.x, streetcar.y));
                    }
                }
            }
        });
    }

    private void drawStops(ArrayList<Stop> stops) {
        for (int i = 0; i < stops.size(); i++) {
            LatLng location = new LatLng(stops.get(i).lat, stops.get(i).lon);
            Marker marker = mMap.addMarker(new MarkerOptions()
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop_icon))
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("stop_icon",50,50)))
                .anchor(0.5f, 0.5f)
                .position(location));

            marker.setTag(stops.get(i).stopId);
        }
    }

    private void drawRouteLines(ArrayList<LatLng> points) {
        mMap.addPolyline(new PolylineOptions()
            .addAll(points)
            .width(10)
            .color(0xB3000000));
    }

    private Bitmap resizeMapIcons(String iconName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    private String convertKmHrToMph(int speed) {
        return Math.round(speed * 0.62137119223733) + " Mph";
    }

    private void getArrivalTime(int stopId) {
        String url = "http://webservices.nextbus.com/service/publicJSONFeed?command=predictions&a=seattle-sc&r=" + "FHS" + "&s=" + stopId;

        WebRequest wr = new WebRequest();
        wr.getArrivalTimes(queue, url, new FetchArrivalTimes() {
            @Override
            public void onTaskCompleted(ArrayList response) {
                Log.v("Response from arrival", response.toString());

//                ArrayList arrivalTimes = new ArrayList<>();

//                for (int i = 0; i < response.length(); i++) {
//                    try {
//                        int arrivalTime = response.getJSONObject(i).optInt("minutes");
//
//                        arrivalTimes.add(arrivalTime);
//                    }
//                    catch (JSONException error) {
//                        Log.v("Error", "There was an error parsing JSON " + error.toString());
//                    }
//                }

                createArrivalText(response);

            }
        });

//        let contentString = "";
//
//        contentString += `<div class="stop-header"><h3>${stop.title}</h3>`;
//        contentString = addFavoriteButton(contentString, stop);
//        contentString += "<h4>Arrivals:</h4>";
//        contentString += "<ol class=\"arrival-info\">";
//
//        for (const arrivalTime of data.predictions.direction.prediction) {
//            contentString += `<li>${arrivalTime.minutes} mins</li>`;
//        }
//
//        contentString += "</ul>";
//
//        stop.infoWindow.setContent(contentString, stop);
//
//        addFavoriteListener(stop);
    }

    private void createArrivalText(final ArrayList arrivalTimes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ll_slide_ui);

                linearLayout.removeAllViews();

                TextView tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setText(arrivalTimes.get(0).toString());
                linearLayout.addView(tv);

                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                tv.setText("Arrivals:");
                linearLayout.addView(tv);


                for (int i = 1; i < arrivalTimes.size(); i++) {
                    tv = new TextView(getApplicationContext());
                    tv.setLayoutParams(lparams);
                    tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                    tv.setText(arrivalTimes.get(i) + " minutes");
                    linearLayout.addView(tv);
                }
            }
        });
    }

}

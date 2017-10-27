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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
        setContentView(R.layout.main_layout_newest);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        queue = Volley.newRequestQueue(this);
        JodaTimeAndroid.init(this);

        LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);
        bottomPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("Clicked", "Click was called on bottom panel");
            }
        });
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
        LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);
        bottomPanel.removeAllViews();
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (marker.getTag() != null) {
            String tag = marker.getTag().toString();

            int id = Integer.parseInt(tag.substring(tag.indexOf(" ") + 1));
            String type = tag.substring(0, tag.indexOf(" "));

            Log.v("Click vars", type + id);

            switch(type) {
                case "stop":
                    getArrivalTime(id);
                    break;
                case "streetcar":
                    createStreetcarText(streetcars.get(streetcars.findByStreetcarId(id)));
                    break;
            }
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        getStops();
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
        streetcar.marker.setTag("streetcar " + streetcar.streetcar_id);

        Log.v("Create marker:", "marker is: " + streetcar.marker.toString());
    }

    public void updateStreetcars() {
        getStreetcars(new Callback() {
            @Override
            public void done() {
                Streetcar streetcar;

                for(int i = 0; i < streetcars.length(); i++) {
                    streetcar = streetcars.get(i);

                    if (streetcar.marker == null) {
                        Log.v("Error: ", "Marker not found! Creating one.");
                        createMarker(streetcar);
                    }

                    streetcar.marker.setRotation(streetcar.heading);
                    streetcar.marker.setSnippet("Location: " + streetcar.x + " " + streetcar.y + "Last Speed " + convertKmHrToMph(streetcar.speedkmhr));
                    LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Spherical();
                    MarkerAnimation.animateMarkerToICS(streetcar.marker, new LatLng(streetcar.x, streetcar.y), latLngInterpolator);
                }

                streetcars.checkForOldStreetcars();
            }
        });
    }

    private void drawStops(ArrayList<Stop> stops) {
        for (int i = 0; i < stops.size(); i++) {
            LatLng location = new LatLng(stops.get(i).lat, stops.get(i).lon);
            Marker marker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("stop_icon",50,50)))
                .anchor(0.5f, 0.5f)
                .position(location));

            marker.setTag("stop " + stops.get(i).stopId);
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

                createArrivalText(response);
            }
        });
    }

    private void createArrivalText(final ArrayList arrivalTimes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lparams.setMargins(15, 5, 5, 15);

                LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);

                bottomPanel.removeAllViews();

                TextView tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setText(arrivalTimes.get(0).toString());
                bottomPanel.addView(tv);

                String arrivalStr = "Arriving in ";

                for (int i = 1; i < arrivalTimes.size(); i++) {
                    arrivalStr += arrivalTimes.get(i) + ", ";
                }

                arrivalStr = arrivalStr.substring(0, arrivalStr.length() - 2) + " minutes";

                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setText(arrivalStr);
                bottomPanel.addView(tv);
            }
        });
    }

    private void createStreetcarText(final Streetcar streetcar) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lparams.setMargins(15, 5, 5, 15);

                LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);

                bottomPanel.removeAllViews();
                TextView tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setText("Last updated: " + streetcar.updated_at);
                Log.v("Height of textview: ", ""+tv.getHeight());
                bottomPanel.addView(tv);

                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setText("Idle time: " + streetcar.idle);
                bottomPanel.addView(tv);

                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setText("Last speed: " + convertKmHrToMph(streetcar.speedkmhr));
                bottomPanel.addView(tv);

                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setText("Location: " + streetcar.x + " " + streetcar.y);
                bottomPanel.addView(tv);
            }
        });
    }
}

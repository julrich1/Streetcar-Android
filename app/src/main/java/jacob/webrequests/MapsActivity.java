package jacob.webrequests;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public Streetcars streetcars = new Streetcars();

    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        queue = Volley.newRequestQueue(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        getStops();
        getStreetcars();
    }

    private void getStreetcars() {
        String url ="http://10.5.81.184:3002/api/streetcars/1";

        WebRequest wr = new WebRequest();
        wr.streetcarJsonRequest(queue, url, new OnTaskCompleted() {
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
                updateStreetcars();
            }
        });
    }

    private void getStops() {
        String url ="http://10.5.81.184:3002/api/routes/1";

        final ArrayList<Stop> stops = new ArrayList<>();

        WebRequest wr = new WebRequest();
        wr.stopsRoutesJsonRequest(queue, url, new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(JSONArray response) {
                Gson gson = new Gson();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        Log.v("Stop response", response.toString());
                        Stop stop = gson.fromJson(response.getJSONObject(i).toString(), Stop.class);
                        stops.add(stop);
                    }
                    catch (JSONException error) {
                        Log.v("Error", "Error getting JSON Object");
                    }
                }

                drawStops(stops);
            }
        });
    }

    public void updateStreetcars() {
        for(int i = 0; i < streetcars.length(); i++) {
            LatLng location = new LatLng(streetcars.get(i).x, streetcars.get(i).y);
            mMap.addMarker(new MarkerOptions().position(location));
        }
    }

    public void drawStops(ArrayList<Stop> stops) {
        for (int i = 0; i < stops.size(); i++) {
//            Stop stop = stops.get(i);
            LatLng location = new LatLng(stops.get(i).lat, stops.get(i).lon);
            mMap.addMarker(new MarkerOptions().position(location));

//            Log.v("In draw", stops.get(i).toString());
        }
    }
}

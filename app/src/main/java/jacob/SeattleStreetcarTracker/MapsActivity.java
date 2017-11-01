package jacob.SeattleStreetcarTracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.Polyline;
import com.google.gson.Gson;

import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.danlew.android.joda.JodaTimeAndroid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// TO-DO: Check for duplicate stop requests when switching routes

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, NavigationView.OnNavigationItemSelectedListener {

    private final String API_URL = "http://sc-dev.shadowline.net";
    private final int SC_UPDATE_INTERVAL = 2000;
    private final int FAVORITE_UPDATE_INTERVAL = 20000;
    private final int STOP_UPDATE_INTERVAL = 20000;
    private final int INFO_UPDATE_INTERVAL = 1000;

    public static final String REQUEST_TAG = "SCRequests";

    private BitmapDescriptor STREETCAR_ICON;
    private BitmapDescriptor STOP_ICON;
    private Bitmap STAR_EMPTY_ICON;
    private Bitmap STAR_FULL_ICON;

    private GoogleMap mMap;
    public Streetcars streetcars = new Streetcars();
    public ArrayList<Stop> stops = new ArrayList<>();
    public ArrayList<Polyline> polylines = new ArrayList<>();
    public FavoriteStops favoriteStops;

    private SelectedItem selectedItem = new SelectedItem();

    private Timer scTimer;
    private Timer favoriteTimer;
    private Timer infoTimer;
    private Timer stopTimer;

    private ActionBarDrawerToggle abToggle;
    private DrawerLayout drawerLayout;
    private NavigationView mNavigationView;

    private RequestQueue queue;

    int route = 1;

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

        favoriteStops = SettingsManager.getFavoriteStops(getApplicationContext());
        route = SettingsManager.loadRoute(getApplicationContext());

        if (favoriteStops == null) {
            favoriteStops = new FavoriteStops();
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Log.v("Display metrics",  metrics.densityDpi +"");


        STREETCAR_ICON = BitmapDescriptorFactory.fromBitmap(ImageHandler.scaleMapIcons(getResources(), getPackageName(), "streetcar", 0.2f, 0.2f));
        STOP_ICON = BitmapDescriptorFactory.fromBitmap(ImageHandler.resizeMapIcons(getResources(), getPackageName(), "stop_icon", 50, 50));
        STAR_EMPTY_ICON = ImageHandler.resizeMapIcons(getResources(), getPackageName(), "star_empty", 80, 80);
        STAR_FULL_ICON = ImageHandler.resizeMapIcons(getResources(), getPackageName(), "star_full", 80, 80);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        abToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(abToggle);

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        if (mNavigationView != null) {
            mNavigationView.setNavigationItemSelectedListener(this);
            drawFavoritesMenu();
        }

        LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);
        bottomPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("Clicked", "Click was called on bottom panel");
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.v("Location", "Permission denied");
            // Permission to access the location is missing.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
        else if (mMap != null) {
            Log.v("Location", "Permission allowed");
            mMap.setMyLocationEnabled(true);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    enableMyLocation();
                }
                return;
            }
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        Log.v("Menu item ", "Was clicked");
        LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);


        if (id == R.id.nav_item_slu) {
            route = 2;
            SettingsManager.saveRoute(getApplicationContext(), route);
            swapViews(item, mNavigationView.getMenu().findItem(R.id.nav_item_fhs), new LatLng(47.621358, -122.338190));
            drawerLayout.closeDrawers();
            bottomPanel.removeAllViews();
            getFavoritesArrivalTimes();
        }
        else if (id == R.id.nav_item_fhs) {
            route = 1;
            SettingsManager.saveRoute(getApplicationContext(), route);
            swapViews(item, mNavigationView.getMenu().findItem(R.id.nav_item_slu), new LatLng(47.609809, -122.320826));
            drawerLayout.closeDrawers();
            bottomPanel.removeAllViews();
            getFavoritesArrivalTimes();
        }

        return true;
    }

    private void drawFavoritesMenu() {
        MenuItem favoriteItem = mNavigationView.getMenu().findItem(R.id.favorite);
        SubMenu subMenu = favoriteItem.getSubMenu();

        ArrayList<FavoriteStop> favObj;

        if (route == 1) {
            favObj = favoriteStops.FHS;
        }
        else {
            favObj = favoriteStops.SLU;
        }

        subMenu.clear();

        if (favObj.size() == 0) {
            subMenu.add(R.id.favorites, 5000, Menu.NONE, "No saved favorites");

            return;
        }

        for (int i = 0; i < favObj.size(); i++) {
            subMenu.add(R.id.favorites, 5000, Menu.NONE, favObj.get(i).stopTitle).setIcon(R.drawable.ic_directions_railway_black_24dp);

            String arrivalText = favObj.get(i).arrivalTimes;

            if (arrivalText == "") {
                arrivalText = "Fetching arrival times";
            }

            subMenu.add(R.id.favorites, 5001, Menu.NONE, arrivalText);
        }
    }

    private void swapViews(MenuItem item, MenuItem oldItem, LatLng routeCenter) {
        queue.cancelAll(REQUEST_TAG);
        removeStops();
        removeRouteLines();
        streetcars.removeStreetcars();
        oldItem.setIcon(null);
        oldItem.setChecked(false);

        item.setIcon(R.drawable.ic_check_black_24dp);

        setCamera(routeCenter);
    }

    private void setCamera(LatLng cameraCenter) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(cameraCenter)
                .zoom(15)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {

        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        abToggle.syncState();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.v("Event pause", "onPause() was called");
        scTimer.cancel();
        favoriteTimer.cancel();
        infoTimer.cancel();
        stopTimer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.v("Event resume", "onResume() was called");

        scTimer = new Timer();
        favoriteTimer = new Timer();
        infoTimer = new Timer();
        stopTimer = new Timer();
        startTimers();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        selectedItem.type = null;
        selectedItem.id = 0;
        selectedItem.lastUpdated = 0;

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
                    selectedItem.lastUpdated = 0;
                    selectedItem.id = id;
                    selectedItem.type = "stop";

                    getArrivalTime(id, new CallbackArrayList() {
                        @Override
                        public void done(ArrayList response) {
                            createArrivalText(response);
                        }
                    });
                    break;
                case "streetcar":
                    int scIndex = streetcars.findByStreetcarId(id);

                    if (scIndex != -1) {
                        selectedItem.type = "streetcar";
                        selectedItem.id = id;

                        createStreetcarText(streetcars.get(scIndex));
                    }
                    break;
            }
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng cameraCenter;

        MenuItem fhs_item = mNavigationView.getMenu().findItem(R.id.nav_item_fhs);
        MenuItem slu_item = mNavigationView.getMenu().findItem(R.id.nav_item_slu);

        if (route == 1) {
            fhs_item.setIcon(R.drawable.ic_check_black_24dp);
            fhs_item.setChecked(true);
            slu_item.setIcon(null);
            cameraCenter = new LatLng(47.609809, -122.320826);
        }
        else {
            slu_item.setIcon(R.drawable.ic_check_black_24dp);
            slu_item.setChecked(true);
            fhs_item.setIcon(null);
            cameraCenter = new LatLng(47.621358, -122.338190);
        }
        setCamera(cameraCenter);

        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);


        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        enableMyLocation();
    }

    private void startTimers() {
        scTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                if (stops.size() == 0) {
                    getStops();
                }

                updateStreetcars();
            }
        }, 0, SC_UPDATE_INTERVAL);

        favoriteTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                getFavoritesArrivalTimes();
            }
        }, 0, FAVORITE_UPDATE_INTERVAL);

        favoriteTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                updateInfoWindows();
            }
        }, 0, INFO_UPDATE_INTERVAL);
    }

    private void removeStops() {
        for (int i = 0; i < stops.size(); i++) {
            stops.get(i).marker.remove();
        }

        stops.clear();
    }

    private void removeRouteLines() {
        for (int i = 0; i < polylines.size(); i++) {
            polylines.get(i).remove();
        }
    }

    private void getStreetcars(final Callback cb) {
        String url = API_URL + "/api/streetcars/" + route;

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
        if (stops.size() != 0) { return; }

        String url = API_URL + "/api/routes/" + route;

        WebRequest wr = new WebRequest();
        wr.stopsRoutesJsonRequest(queue, url, new FetchedStopsAndRoutes() {
            @Override
            public void onTaskCompleted(JSONArray stopsArray, JSONArray paths) {
                Gson gson = new Gson();

                for (int i = 0; i < stopsArray.length(); i++) {
                    try {
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

                drawStops();
            }
        });
    }

    private MarkerOptions setMarkerOptions(Streetcar streetcar) {
        LatLng location = new LatLng(streetcar.x, streetcar.y);

        return new MarkerOptions()
            .position(location)
            .icon(STREETCAR_ICON)
            .anchor(0.5f, 0.5f)
            .rotation(streetcar.heading)
            .zIndex(1.0f);
    }

    private void createMarker(Streetcar streetcar) {
        streetcar.marker = mMap.addMarker(setMarkerOptions(streetcar));
        streetcar.marker.setTag("streetcar " + streetcar.streetcar_id);
    }

    private void updateInfoWindows() {
        if (selectedItem.type == "streetcar") {
            int id = streetcars.findByStreetcarId(selectedItem.id);

            if (id != -1) {
                createStreetcarText(streetcars.get(id));
            }
        }
        else if (selectedItem.type == "stop") {
            selectedItem.lastUpdated++;

            if (selectedItem.lastUpdated >= 20) {
                selectedItem.lastUpdated = 0;
                getArrivalTime(selectedItem.id, new CallbackArrayList() {
                    @Override
                    public void done(ArrayList response) {
                        createArrivalText(response);
                    }
                });
            }
        }
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

                    if (streetcar.marker.getRotation() != streetcar.heading) {
                        streetcar.marker.setRotation(streetcar.heading);
                    }

                    LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Spherical();
                    MarkerAnimation.animateMarkerToICS(streetcar.marker, new LatLng(streetcar.x, streetcar.y), latLngInterpolator);
                }

                streetcars.checkForOldStreetcars();
            }
        });
    }

    private void drawStops() {
        for (int i = 0; i < stops.size(); i++) {
            LatLng location = new LatLng(stops.get(i).lat, stops.get(i).lon);
            Marker marker = mMap.addMarker(new MarkerOptions()
                .icon(STOP_ICON)
                .anchor(0.5f, 0.5f)
                .position(location));

            marker.setTag("stop " + stops.get(i).stopId);
            stops.get(i).marker = marker;
        }
    }

    private void drawRouteLines(ArrayList<LatLng> points) {
        Polyline pl = mMap.addPolyline(new PolylineOptions()
            .addAll(points)
            .width(10)
            .color(0x7F000000));

        polylines.add(pl);
    }

    private void getArrivalTime(int stopId, final CallbackArrayList cb) {
        String routeString;

        if (route == 1) { routeString = "FHS"; }
        else { routeString = "SLU"; }

        String url = "http://webservices.nextbus.com/service/publicJSONFeed?command=predictions&a=seattle-sc&r=" + routeString + "&s=" + stopId;

        WebRequest wr = new WebRequest();
        wr.getArrivalTimes(queue, url, new FetchArrivalTimes() {
            @Override
            public void onTaskCompleted(ArrayList response) {
                Log.v("Response from arrival", response.toString());

                cb.done(response);
            }
        });
    }

    private void createArrivalText(final ArrayList arrivalTimes) {
        final LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);

        if (arrivalTimes.size() == 0) {
            bottomPanel.removeAllViews();
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int margin = convertDpToPx(10);

                /// Create new linearlayout to contain the stop name and star icon
                LinearLayout stopNameLayout = new LinearLayout(getApplicationContext());
                stopNameLayout.setLayoutParams(new LinearLayoutCompat.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                stopNameLayout.setOrientation(LinearLayout.HORIZONTAL);
                /// End creation of linearlayout

                bottomPanel.removeAllViews();

                /// Create stop title text and params
                LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
                tvParams.setMargins(margin, margin, 0, 0);

                TextView tv = new TextView(getApplicationContext());
                tv.setText(arrivalTimes.get(0).toString());
                tv.setLayoutParams(tvParams);


                if (Build.VERSION.SDK_INT < 23) {
                    tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_AppCompat_Large);
                } else {
                    tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                }
//                tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_AppCompat_Large);
//                tv.setBackgroundColor(0xFFFF0000);

                tv.setTextColor(Color.WHITE);


                stopNameLayout.addView(tv);
                /// End creating stop title text and params


                /// Create star icon and params
                LinearLayout.LayoutParams starIconParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                starIconParams.setMarginEnd(margin);
                starIconParams.setMargins(0, margin, 0, 0);
                starIconParams.gravity = Gravity.CENTER_VERTICAL;

                ImageView emptyStar = new ImageView(getApplicationContext());
                emptyStar.setLayoutParams(starIconParams);

                if (favoriteStops.isFavorited((int) arrivalTimes.get(1), route)) {
                    emptyStar.setImageBitmap(STAR_FULL_ICON);
                }
                else {
                    emptyStar.setImageBitmap(STAR_EMPTY_ICON);
                }

                emptyStar.setTag(new Stop((int) arrivalTimes.get(1), (String) arrivalTimes.get(0)));

                emptyStar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v("Clicked", "Click was called on the star!");
                        ImageView star = (ImageView) v;

                        Stop clickedStop = (Stop) star.getTag();

                        if (favoriteStops.isFavorited(clickedStop.stopId, route)) {
                            favoriteStops.removeFavorite(clickedStop.stopId, route);
                            star.setImageBitmap(STAR_EMPTY_ICON);
                            SettingsManager.saveFavoriteStops(getApplicationContext(), favoriteStops);
                        }
                        else {
                            favoriteStops.addFavorite(clickedStop.stopId, clickedStop.title, route);
                            star.setImageBitmap(STAR_FULL_ICON);
                            SettingsManager.saveFavoriteStops(getApplicationContext(), favoriteStops);
                            getFavoritesArrivalTimes();
                        }

                        drawFavoritesMenu();
                    }
                });

                stopNameLayout.addView(emptyStar);
                // End creating star icon and params

                bottomPanel.addView(stopNameLayout);

                // Create arrival text
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                lparams.setMargins(margin, 0, 0, margin);
                String arrivalStr = "Arriving in ";

                for (int i = 2; i < arrivalTimes.size(); i++) {
                    arrivalStr += arrivalTimes.get(i) + ", ";
                }

                arrivalStr = arrivalStr.substring(0, arrivalStr.length() - 2) + " minutes";

                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(lparams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                tv.setTextColor(Color.WHITE);
                tv.setText(arrivalStr);
                bottomPanel.addView(tv);
            }
        });
    }

    private int convertDpToPx(int dp){
        return Math.round(dp*(getResources().getDisplayMetrics().xdpi/ DisplayMetrics.DENSITY_DEFAULT));
    }


    private void createStreetcarText(final Streetcar streetcar) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int margin = convertDpToPx(10);

                LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);

                bottomPanel.removeAllViews();
                TextView tv;

//                TextView tv = new TextView(getApplicationContext());
//                tv.setLayoutParams(lparams);
//                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
//                tv.setText("Last updated: " + streetcar.updated_at);
//                bottomPanel.addView(tv);

                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                headerParams.setMargins(margin, margin, 0, 0);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(headerParams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                tv.setTextColor(Color.WHITE);
                tv.setText("Streetcar");
                bottomPanel.addView(tv);

                LinearLayout.LayoutParams idleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                idleParams.setMargins(margin, 0, 0, 0);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(idleParams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                tv.setTextColor(Color.WHITE);
                tv.setText("Idle time: " + streetcar.idle);
                bottomPanel.addView(tv);

                LinearLayout.LayoutParams speedParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                speedParams.setMargins(margin, 0, 0, 0);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(speedParams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                tv.setTextColor(Color.WHITE);
                tv.setText("Last speed: " + streetcars.convertKmHrToMph(streetcar.speedkmhr));
                bottomPanel.addView(tv);

                LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                locationParams.setMargins(margin, 0, 0, margin);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(locationParams);
                tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                tv.setTextColor(Color.WHITE);
                tv.setText("Location: " + streetcar.x + " " + streetcar.y);
                bottomPanel.addView(tv);
            }
        });
    }

    private void getFavoritesArrivalTimes() {
        ArrayList<FavoriteStop> favObj;

        if (route == 1) {
            favObj = favoriteStops.FHS;
        }
        else {
            favObj = favoriteStops.SLU;
        }

        if (favObj.size() == 0) {
            drawFavoritesMenu();
            return;
        }
        else if (favObj.size() == 1) {
            getArrivalTime(favObj.get(0).stopId, new CallbackArrayList() {
                @Override
                public void done(ArrayList response) {
                    String arrivalStr = "";

                    for (int i = 2; i < response.size(); i++) {
                        arrivalStr += response.get(i) + ", ";
                    }

                    arrivalStr = arrivalStr.substring(0, arrivalStr.length() - 2) + " minutes";

                    favoriteStops.searchByStopId((int) response.get(1), route).arrivalTimes = arrivalStr;

                    Log.v("Favorite Event", "Favorite size is 1, and response received. Calling drawFavoritesMenu()");
                    drawFavoritesMenu();
                }
            });
        }
        else {
            favoriteStops.getQueryString(route);

            String url = "http://webservices.nextbus.com/service/publicJSONFeed?command=predictionsForMultiStops&a=seattle-sc" + favoriteStops.getQueryString(route);

            WebRequest wr = new WebRequest();
            wr.getMultipleFavoriteArrivalTimes(queue, url, new FetchAllArrivalTimes() {
                @Override
                public void onTaskCompleted(ArrayList<ArrayList> response) {
                    Log.v("Final response", response.toString());
                    favoriteStops.addArrivalTimes(response, route);
                    drawFavoritesMenu();
                }
            });
        }
    }
}

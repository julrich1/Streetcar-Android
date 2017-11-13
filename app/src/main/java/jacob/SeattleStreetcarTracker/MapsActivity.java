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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
    public static final String ARRIVAL_REQUEST_TAG = "SCArrivals";

    private BitmapDescriptor STREETCAR_ICON;
    private BitmapDescriptor STREETCAR_SELECTED_ICON;
    private BitmapDescriptor STREETCAR_TRANSPARENT_ICON;
    private BitmapDescriptor STOP_ICON;
    private BitmapDescriptor STOP_SELECTED_ICON;
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

    private LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Spherical();


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

        STREETCAR_ICON = BitmapDescriptorFactory.fromBitmap(ImageHandler.createBitmap(getResources(), getPackageName(), "streetcar"));
        STREETCAR_SELECTED_ICON = BitmapDescriptorFactory.fromBitmap(ImageHandler.createBitmap(getResources(), getPackageName(), "streetcar_selected"));
        STREETCAR_TRANSPARENT_ICON = BitmapDescriptorFactory.fromBitmap(ImageHandler.createBitmap(getResources(), getPackageName(), "streetcar_transparent"));
        STOP_ICON = BitmapDescriptorFactory.fromBitmap(ImageHandler.createBitmap(getResources(), getPackageName(), "stop_icon"));
        STOP_SELECTED_ICON = BitmapDescriptorFactory.fromBitmap(ImageHandler.createBitmap(getResources(), getPackageName(), "stop_icon_selected"));
        STAR_EMPTY_ICON = ImageHandler.createBitmap(getResources(), getPackageName(), "star_empty");
        STAR_FULL_ICON = ImageHandler.createBitmap(getResources(), getPackageName(), "star_full");

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
                // Blocks closing the info window if open.
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else if (mMap != null) {
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    private void swapViews(MenuItem item, MenuItem oldItem, LatLng routeCenter) {
        queue.cancelAll(REQUEST_TAG);
        queue.cancelAll(ARRIVAL_REQUEST_TAG);
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

        scTimer.cancel();
        favoriteTimer.cancel();
        infoTimer.cancel();
        stopTimer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        scTimer = new Timer();
        favoriteTimer = new Timer();
        infoTimer = new Timer();
        stopTimer = new Timer();
        startTimers();
    }

    public void swapStreetcarIcon(int id, BitmapDescriptor newIcon) {
        int scIndex = streetcars.findByStreetcarId(id);

        if (scIndex != -1) {
            LatLng currentPosition = streetcars.get(scIndex).marker.getPosition();
            LatLng targetPosition = new LatLng(streetcars.get(scIndex).x, streetcars.get(scIndex).y);
            streetcars.get(scIndex).marker.remove();

            streetcars.get(scIndex).x = (float) currentPosition.latitude;
            streetcars.get(scIndex).y = (float) currentPosition.longitude;

            createMarker(streetcars.get(scIndex), newIcon);

            MarkerAnimation.animateMarkerToICS(streetcars.get(scIndex).marker, targetPosition, latLngInterpolator);

        }
    }

    public void swapStopIcon(int id, BitmapDescriptor newIcon) {
        Stop stop = null;

        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).stopId == id) {
                stop = stops.get(i);
                break;
            }
        }

        if (stop != null) {
            stop.marker.remove();
            createStopMarker(stop, newIcon);
        }
    }

        @Override
    public void onMapClick(LatLng latLng) {
        if (selectedItem.type == "streetcar") {
            BitmapDescriptor icon = STREETCAR_ICON;

            int index = streetcars.findByStreetcarId(selectedItem.id);
            if (index != -1) {
                if (streetcars.get(index).predictable == false) {
                    icon = STREETCAR_TRANSPARENT_ICON;
                }
            }
            swapStreetcarIcon(selectedItem.id, icon);
        }
        else if (selectedItem.type == "stop") {
            swapStopIcon(selectedItem.id, STOP_ICON);
        }

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

            if (selectedItem.id != id) {
                if (selectedItem.type == "streetcar") {
                    BitmapDescriptor icon = STREETCAR_ICON;

                    int index = streetcars.findByStreetcarId(selectedItem.id);
                    if (index != -1) {
                        if (streetcars.get(index).predictable == false) {
                            icon = STREETCAR_TRANSPARENT_ICON;
                        }
                    }

                    swapStreetcarIcon(selectedItem.id, icon);
                }
                else if (selectedItem.type == "stop") {
                    swapStopIcon(selectedItem.id, STOP_ICON);
                }
            }

            switch(type) {
                case "stop":
                    selectedItem.lastUpdated = 0;
                    selectedItem.id = id;
                    selectedItem.type = "stop";

                    swapStopIcon(id, STOP_SELECTED_ICON);

                    drawProgressBar();

                    queue.cancelAll(ARRIVAL_REQUEST_TAG);
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

                        swapStreetcarIcon(id, STREETCAR_SELECTED_ICON);

                        drawProgressBar();

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
                        if (stop.stopId != 0) {
                            stops.add(stop);
                        }
                    }
                    catch (JSONException error) {
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
                    }
                }

                drawStops();
            }
        });
    }

    private MarkerOptions setMarkerOptions(Streetcar streetcar, BitmapDescriptor icon) {
        LatLng location = new LatLng(streetcar.x, streetcar.y);

        return new MarkerOptions()
            .position(location)
            .icon(icon)
            .anchor(0.5f, 0.5f)
            .rotation(streetcar.heading)
            .zIndex(1.0f);
    }

    private void createMarker(Streetcar streetcar, BitmapDescriptor icon) {
        streetcar.marker = mMap.addMarker(setMarkerOptions(streetcar, icon));
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
                        BitmapDescriptor icon = STREETCAR_ICON;

                        if (streetcar.predictable == false) {
                            icon = STREETCAR_TRANSPARENT_ICON;
                        }

                        createMarker(streetcar, icon);
                    }

                    if (streetcar.marker.getRotation() != streetcar.heading) {
                        streetcar.marker.setRotation(streetcar.heading);
                    }

                    MarkerAnimation.animateMarkerToICS(streetcar.marker, new LatLng(streetcar.x, streetcar.y), latLngInterpolator);
                }

                streetcars.checkForOldStreetcars();
            }
        });
    }

    private void drawStops() {
        for (int i = 0; i < stops.size(); i++) {
            createStopMarker(stops.get(i), STOP_ICON);
        }
    }

    private Marker createStopMarker(Stop stop, BitmapDescriptor icon) {
        LatLng location = new LatLng(stop.lat, stop.lon);

        Marker marker = mMap.addMarker(new MarkerOptions()
                .icon(icon)
                .anchor(0.5f, 0.5f)
                .position(location));

        marker.setTag("stop " + stop.stopId);

        stop.marker = marker;

        return marker;
    }

    private void drawRouteLines(ArrayList<LatLng> points) {
        Polyline pl = mMap.addPolyline(new PolylineOptions()
            .addAll(points)
            .width(10)
            .color(0x7F000000));

        polylines.add(pl);
    }

    private void getArrivalTime(int stopId, final CallbackArrayList cb) {
        String url = API_URL + "/api/routes/" + route + "/arrivals/" + stopId;

        WebRequest wr = new WebRequest();
        wr.getArrivalTimes(queue, url, new FetchArrivalTimes() {
            @Override
            public void onTaskCompleted(ArrayList response) {
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

                if (Build.VERSION.SDK_INT < 23) {
                    tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_AppCompat_Medium);
                } else {
                    tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                }

                tv.setTextColor(Color.WHITE);
                tv.setText(arrivalStr);

                bottomPanel.removeAllViews();

                bottomPanel.addView(stopNameLayout);

                bottomPanel.addView(tv);
            }
        });
    }

    private int convertDpToPx(int dp){
        return Math.round(dp*(getResources().getDisplayMetrics().xdpi/ DisplayMetrics.DENSITY_DEFAULT));
    }

    private void drawProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);

                bottomPanel.removeAllViews();

                ProgressBar pbar = new ProgressBar(getApplicationContext(), null, android.R.attr.progressBarStyleLarge);

                pbar.setIndeterminate(true);
                pbar.setVisibility(View.VISIBLE);

                bottomPanel.addView(pbar);
            }
        });
    }


    private void createStreetcarText(final Streetcar streetcar) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int margin = convertDpToPx(10);
                int marginIndent = convertDpToPx(30);

                LinearLayout bottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);

                bottomPanel.removeAllViews();
                TextView tv;

                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                headerParams.setMargins(margin, margin, 0, 0);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(headerParams);

                if (Build.VERSION.SDK_INT < 23) {
                    tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_AppCompat_Large);
                } else {
                    tv.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                }
                tv.setTextColor(Color.WHITE);
                tv.setText("Streetcar");
                bottomPanel.addView(tv);

                LinearLayout.LayoutParams idleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                idleParams.setMargins(marginIndent, 0, 0, 0);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(idleParams);
                if (Build.VERSION.SDK_INT < 23) {
                    tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_AppCompat_Medium);
                } else {
                    tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                }
                tv.setTextColor(Color.WHITE);
                tv.setText("Idle time: " + streetcar.idle);
                bottomPanel.addView(tv);

                LinearLayout.LayoutParams speedParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                speedParams.setMargins(marginIndent, 0, 0, 0);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(speedParams);
                if (Build.VERSION.SDK_INT < 23) {
                    tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_AppCompat_Medium);
                } else {
                    tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                }
                tv.setTextColor(Color.WHITE);
                tv.setText("Last speed: " + streetcars.convertKmHrToMph(streetcar.speedkmhr));
                bottomPanel.addView(tv);

                LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                locationParams.setMargins(marginIndent, 0, 0, margin);
                tv = new TextView(getApplicationContext());
                tv.setLayoutParams(locationParams);
                if (Build.VERSION.SDK_INT < 23) {
                    tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_AppCompat_Medium);
                } else {
                    tv.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                }
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

                    drawFavoritesMenu();
                }
            });
        }
        else {
            String url = API_URL + "/api/routes/" + route + "/arrivals/" + favoriteStops.getQueryString(route);

            WebRequest wr = new WebRequest();
            wr.getMultipleFavoriteArrivalTimes(queue, url, new FetchAllArrivalTimes() {
                @Override
                public void onTaskCompleted(ArrayList<ArrayList> response) {
                    favoriteStops.addArrivalTimes(response, route);
                    drawFavoritesMenu();
                }
            });
        }
    }
}

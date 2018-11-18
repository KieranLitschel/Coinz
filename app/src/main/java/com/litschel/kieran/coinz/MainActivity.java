package com.litschel.kieran.coinz;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import android.location.Location;

import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener, MapDownloadedCallback {

    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private SharedPreferences settings;
    private final String settingsFile = "SettingsFile";
    private boolean justStarted;
    private ArrayList<Marker> markers;
    private Timer myTimer;
    private Handler myTimerTaskHandler;
    private static final int RC_SIGN_IN = 9000;
    private String uid;
    private LocationManager locationManager;
    private Context mContext;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        justStarted = true;

        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 0);
        } else {
            System.out.println("PERMISSION ACESS_NETWORK_STATE IS GRANTED");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        } else {
            System.out.println("PERMISSION INTERNET IS GRANTED");
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            enableLocationPlugin();
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // GET RID OF BEFORE SUBMISSION
                ArrayList<Marker> markersToRemove = new ArrayList<>();
                markersToRemove.add(markers.get(0));
                removeMarkers(markersToRemove);
            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
            locationPlugin.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation == null) {
            locationEngine.addLocationEngineListener(this);
        }

        LatLngBounds PLAY_BOUNDS = new LatLngBounds.Builder()
                .include(new LatLng(55.946233, -3.192473))
                .include(new LatLng(55.946233, -3.184319))
                .include(new LatLng(55.942617, -3.192473))
                .include(new LatLng(55.942617, -3.184319))
                .build();
        map.setLatLngBoundsForCameraTarget(PLAY_BOUNDS);

        settings = getSharedPreferences(settingsFile, Context.MODE_PRIVATE);
        uid = settings.getString("uid", "");

        System.out.println("GOT UID OF "+uid);

        db = FirebaseFirestore.getInstance();

        if (uid.equals("")) {
            signIn();
        }

        if (justStarted && (!settings.getString("map", "").equals(""))) {
            updateMarkers();
            justStarted = false;
        }

        checkForMapUpdate();

        mContext = this;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000,
                10, locationListenerGPS);
    }

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            if (markers != null) {
                ArrayList<Marker> markersToRemove = new ArrayList<>();
                for (Marker marker : markers) {
                    LatLng markerPos = marker.getPosition();
                    if (DistanceCalculator.distance(latitude, longitude,
                            markerPos.getLatitude(), markerPos.getLongitude(), "K") * 1000 <= 25) {
                        markersToRemove.add(marker);
                    }
                }
                removeMarkers(markersToRemove);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    // I made this a seperate method to make testing easier
    private void removeMarkers(ArrayList<Marker> markersToRemove) {
        String mapJSONString = settings.getString("map", "");
        HashMap<Marker, String[]> markerDetails = new HashMap<>();

        try {
            JSONObject mapJSON = new JSONObject(mapJSONString);
            JSONArray markersJSON = mapJSON.getJSONArray("features");
            int removed = 0;
            int i = 0;
            while (i < markersJSON.length() && removed < markersToRemove.size()) {
                JSONObject markerJSON = markersJSON.getJSONObject(i);
                for (Marker marker : markersToRemove) {
                    if (marker.getTitle().equals(markerJSON.getJSONObject("properties").getString("id"))) {
                        markerDetails.put(marker, new String[]{
                                markerJSON.getJSONObject("properties").getString("currency"),
                                markerJSON.getJSONObject("properties").getString("value")
                        });
                        markersJSON.remove(i);
                        i--;
                        removed++;

                        break;
                    }
                }
                i++;
            }
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("map", mapJSON.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (Marker marker : markersToRemove) {
            map.removeMarker(marker);
            markers.remove(marker);
            String currency = markerDetails.get(marker)[0];
            float value = Integer.parseInt(markerDetails.get(marker)[1]);
            Snackbar.make(findViewById(R.id.toolbar), "Collected " + value
                    + " " + currency, Snackbar.LENGTH_LONG).show();
            System.out.println("Removed marker " + marker.getTitle());
        }
    }

    private void setToUpdateAtMidnight() {
        TimerTask mTt = new TimerTask() {
            public void run() {
                myTimerTaskHandler.post(new Runnable() {
                    public void run() {
                        updateMap();
                        setToUpdateAtMidnight();
                    }
                });
            }
        };
        try {
            myTimer.schedule(mTt, new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
                    .parse(LocalDate.now().plusDays(1).toString() + " 00:00:00"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void checkForMapUpdate() {
        if (settings != null) {
            LocalDate lastDownloadDate = LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString()));

            if (lastDownloadDate.isBefore(LocalDate.now())) {
                updateMap();
            } else {
                System.out.println("MAP IS UP TO DATE");
            }
        }
    }

    public void updateMap() {
        map.clear(); // Clear the map before updating it to ensure that if there's no internet user can't play with old map
        LocalDate today = LocalDate.now();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("map", "");
        editor.apply();
        String year = String.valueOf(today.getYear());
        String month = String.valueOf(today.getMonthValue());
        if (month.length() == 1) {
            month = "0" + month;
        }
        String day = String.valueOf(today.getDayOfMonth());
        if (day.length() == 1) {
            day = "0" + day;
        }
        String url = String.format("http://homepages.inf.ed.ac.uk/stg/coinz/%s/%s/%s/coinzmap.geojson", year, month, day);
        System.out.println("DOWNLOADING FROM URL: " + url);
        if (!isNetworkAvailable()) {
            Snackbar.make(findViewById(R.id.toolbar), "Will update map when there is an internet connection", Snackbar.LENGTH_LONG)
                    .show();
            setToUpdateOnInternet();
        }
        new DownloadMapTask(this).execute(url);
    }

    private void setToUpdateOnInternet() {
        TimerTask mTtInternet = new TimerTask() {
            public void run() {
                myTimerTaskHandler.post(() -> {
                    if (isNetworkAvailable()) {
                        updateMap();
                    } else {
                        setToUpdateOnInternet();
                    }
                });
            }
        };
        myTimer.schedule(mTtInternet, 5000);
    }

    // I found this method here (https://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android)
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            finish();
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            locationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    protected void onStart() {
        super.onStart();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myTimer = new Timer();
        myTimerTaskHandler = new Handler();
        mapView.onResume();
        checkForMapUpdate();
        setToUpdateAtMidnight();
    }

    private void signIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("uid", uid);
                editor.apply();
                checkFirstTimeUser();
                System.out.println("USER SIGNED IN SUCCESSFULLY");
                // ...
            } else {
                if (response != null) {
                    System.out.println("USER FAILED TO SIGN IN WITH ERROR CODE" + response.getError().getErrorCode());
                    Snackbar.make(findViewById(R.id.toolbar), String.format("Failed to sign in with error code %s, please try again", response.getError().getErrorCode()), Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    Snackbar.make(findViewById(R.id.toolbar), "You must sign in to use this app.", Snackbar.LENGTH_LONG)
                            .show();
                    signIn();
                }
            }
        }
    }

    private void checkFirstTimeUser(){
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    System.out.println("FIRST TIME USER, SETTING DEFAULTS");
                    setFirstTimeUser();
                } else {
                    System.out.println("USER EXISTS IN DATABASE");
                }
            } else {
                System.out.println("FAILED TO GET WHETHER USER IS LOGGED IN");
            }
        });
    }

    private void setFirstTimeUser(){
        Map<String,Object> user_defaults = new HashMap<>();
        user_defaults.put("DOLR",0.0);
        user_defaults.put("GOLD",0.0);
        user_defaults.put("PENY",0.0);
        user_defaults.put("QUID",0.0);
        user_defaults.put("SHIL",0.0);
        db.collection("users").document(uid)
                .set(user_defaults)
                .addOnSuccessListener(aVoid -> System.out.println("SUCCESSFULLY ADDED USER TO DATABASE"))
                .addOnFailureListener(e -> System.out.println("FAILED TO ADD USER TO DATABASE"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        myTimer.cancel();
        myTimer.purge();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_reset_view:
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(55.944425, -3.188396))
                        .zoom(15)
                        .bearing(0)
                        .tilt(0)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
                return true;
            case R.id.action_logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logout(){
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("uid", "");
        editor.apply();
        uid = "";
        signIn();
    }

    @Override
    public void onMapDownloaded(String mapJSONString) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("map", mapJSONString);
        editor.putString("lastDownloadDate", LocalDate.now().toString());
        editor.apply();
        System.out.println("SUCCEEDED IN DOWNLOADING MAP");
        updateMarkers(mapJSONString);
    }

    public void updateMarkers() {
        String mapJSONString = settings.getString("map", "");
        map.clear();
        updateMarkers(mapJSONString);
    }

    public void updateMarkers(String mapJSONString) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("map", mapJSONString);
        editor.putString("lastDownloadDate", LocalDate.now().toString());
        editor.apply();
        markers = new ArrayList<>();
        if (!mapJSONString.equals("")) {
            try {
                JSONObject mapJSON = new JSONObject(mapJSONString);
                JSONArray markersJSON = mapJSON.getJSONArray("features");
                for (int i = 0; i < markersJSON.length(); i++) {
                    JSONObject markerJSON = markersJSON.getJSONObject(i);
                    JSONArray pos = markerJSON.getJSONObject("geometry").getJSONArray("coordinates");
                    Icon icon = Methods.drawableToIcon(this, R.drawable.marker_icon,
                            Color.parseColor(markerJSON.getJSONObject("properties").getString("marker-color")));
                    MarkerOptions markerOption = new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(pos.getString(1)),
                                    Double.parseDouble(pos.getString(0))))
                            .title(markerJSON.getJSONObject("properties").getString("id"))
                            .icon(icon);
                    markers.add(markerOption.getMarker());
                    map.addMarker(markerOption);
                }
                System.out.println("ADDED NEW MARKERS TO MAP");
                System.out.printf("Added %s markers to the map\n", markersJSON.length());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

interface MapDownloadedCallback {
    void onMapDownloaded(String mapJSONString);
}

class DownloadMapTask extends AsyncTask<String, Void, String> {
    private MapDownloadedCallback context;

    DownloadMapTask(MapDownloadedCallback context) {
        super();
        this.context = context;
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            return loadFileFromNetwork(urls[0]);
        } catch (IOException e) {
            return "FAILED";
        }
    }

    private String loadFileFromNetwork(String urlString) throws IOException {
        return readStream(downloadUrl(new URL(urlString)));
    }

    private InputStream downloadUrl(URL url) throws IOException {
        System.out.println("Making GET request to JSON server");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000); // milliseconds
        conn.setConnectTimeout(15000); // milliseconds
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        System.out.println("Got response from JSON server");
        return conn.getInputStream();
    }

    @NonNull
    private String readStream(InputStream stream)
            throws IOException {
        // Read input from stream, build result as a string
        System.out.println("Started processing JSON response");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder result = new StringBuilder();
        String line;
        line = reader.readLine();
        result.append(line);
        while ((line = reader.readLine()) != null) {
            result.append("\n");
            result.append(line);
        }
        return result.toString();
    }

    @Override
    protected void onPostExecute(String result) {
        System.out.println("Finished processing JSON response");
        super.onPostExecute(result);
        DownloadCompleteRunner.downloadMapComplete(result, context);
    }
}

class DownloadCompleteRunner {

    static void downloadMapComplete(String mapJSONString, MapDownloadedCallback context) {
        if (!(mapJSONString.equals("FAILED"))) {
            context.onMapDownloaded(mapJSONString);
        } else {
            System.out.println("FAILED TO DOWNLOAD MAP");
        }
    }
}

class Methods {
    // I found this method here https://stackoverflow.com/questions/37805379/mapbox-for-android-changing-color-of-a-markers-icon
    static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id, @ColorInt int colorRes) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(vectorDrawable, colorRes);
        vectorDrawable.draw(canvas);
        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }
}

// Found this class for calculating distance between lat and longs here https://www.geodatasource.com/developers/java

class DistanceCalculator {
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::                                                                         :*/
    /*::  This routine calculates the distance between two points (given the     :*/
    /*::  latitude/longitude of those points). It is being used to calculate     :*/
    /*::  the distance between two locations using GeoDataSource (TM) prodducts  :*/
    /*::                                                                         :*/
    /*::  Definitions:                                                           :*/
    /*::    South latitudes are negative, east longitudes are positive           :*/
    /*::                                                                         :*/
    /*::  Passed to function:                                                    :*/
    /*::    lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)  :*/
    /*::    lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)  :*/
    /*::    unit = the unit you desire for results                               :*/
    /*::           where: 'M' is statute miles (default)                         :*/
    /*::                  'K' is kilometers                                      :*/
    /*::                  'N' is nautical miles                                  :*/
    /*::  Worldwide cities and other features databases with latitude longitude  :*/
    /*::  are available at https://www.geodatasource.com                          :*/
    /*::                                                                         :*/
    /*::  For enquiries, please contact sales@geodatasource.com                  :*/
    /*::                                                                         :*/
    /*::  Official Web site: https://www.geodatasource.com                        :*/
    /*::                                                                         :*/
    /*::           GeoDataSource.com (C) All Rights Reserved 2017                :*/
    /*::                                                                         :*/
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/

    public static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }

        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts decimal degrees to radians						 :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts radians to decimal degrees						 :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}
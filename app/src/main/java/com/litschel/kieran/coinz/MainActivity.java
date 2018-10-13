package com.litschel.kieran.coinz;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.mapbox.mapboxsdk.Mapbox;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener {

    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private SharedPreferences settings;
    private final String settingsFile = "SettingsFile";
    private Awake awake;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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

        checkForMapUpdate();
        awake = new Awake();
        new WaitForMidnight(settings, awake).execute();
    }

    private void checkForMapUpdate() {
        LocalDate lastDownloadDate = LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString()));

        if (lastDownloadDate.isBefore(LocalDate.now())) {
            Methods.updateMap(settings);
        } else {
            System.out.println("MAP IS UP TO DATE");
        }
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
        settings = getSharedPreferences(settingsFile, Context.MODE_PRIVATE);
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
        mapView.onResume();
        checkForMapUpdate();
        if (awake != null) {
            awake.wakeUp();
            if (!awake.waitingForMidinight()) {
                awake.setWaitingForMidnight();
                new WaitForMidnight(settings, awake).execute();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (awake != null) {
            awake.goToSleep();
        }
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
        if (awake != null) {
            awake.goToSleep();
        }
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
        if (awake != null) {
            awake.goToSleep();
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
            case R.id.action_settings:
                return true;
            case R.id.action_reset_view:
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(55.944425, -3.188396))
                        .zoom(15)
                        .bearing(0)
                        .tilt(0)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

// Detects midnight and sleeps well, but doesn't update the map for some reason
class WaitForMidnight extends AsyncTask<Void, Void, String> {
    private SharedPreferences settings;
    private Awake awake;

    WaitForMidnight(SharedPreferences settings, Awake awake) {
        super();
        this.settings = settings;
        this.awake = awake;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            boolean waitingForMapUpdate = false;
            System.out.println("WAITINGFORMIDINIGHT: STARTED WAITING");
            while (awake.isAwake()) {
                Thread.sleep(1000);
                String lastUpdate = settings.getString("lastDownloadDate", "");
                if (!lastUpdate.equals("")) {
                    if (LocalDate.parse(lastUpdate).isBefore(LocalDate.now())) {
                        if (!waitingForMapUpdate){
                            System.out.println("WAITINGFORMIDNIGHT: ITS MIDNIGHT, UPDATING MAP");
                            Methods.updateMap(settings);
                            waitingForMapUpdate = true;
                        }
                    } else if (waitingForMapUpdate){
                        waitingForMapUpdate = false;
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.printf("EXCEPTION WHEN WAITINGFORMIDNIGHT, EXCEPTION IS:\n%s\n", e);
        }
        awake.setNotWaitingForMidnight();
        System.out.println("WAITINGFORMIDINIGHT: STOPPED WAITING");
        return "";
    }
}

class Methods{
    public static void updateMap(SharedPreferences settings) {
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
        new DownloadMapTask(settings).execute(url);
    }
}

class DownloadMapTask extends AsyncTask<String, Void, String> {
    private SharedPreferences settings;

    DownloadMapTask(SharedPreferences settings) {
        super();
        this.settings = settings;
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
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000); // milliseconds
        conn.setConnectTimeout(15000); // milliseconds
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        return conn.getInputStream();
    }

    @NonNull
    private String readStream(InputStream stream)
            throws IOException {
        // Read input from stream, build result as a string
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
        super.onPostExecute(result);
        DownloadCompleteRunner.downloadMapComplete(result, settings);
    }
}

class DownloadCompleteRunner {

    static void downloadMapComplete(String result, SharedPreferences settings) {
        if (!(result.equals("FAILED"))) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("map", result);
            editor.putString("lastDownloadDate", LocalDate.now().toString());
            editor.apply();
            System.out.println("SUCEEDED IN DOWNLOADING MAP");
        } else {
            System.out.println("FAILED TO DOWNLOAD MAP");
        }
    }
}

class Awake {
    private boolean awake;
    private boolean waiting;

    Awake() {
        awake = true;
        waiting = true;
    }

    public void goToSleep() {
        awake = false;
    }

    public void wakeUp() {
        awake = true;
    }

    public boolean isAwake() {
        return awake;
    }

    public void setNotWaitingForMidnight() {
        waiting = false;
    }

    public void setWaitingForMidnight() {
        waiting = true;
    }

    public boolean waitingForMidinight() {
        return waiting;
    }
}
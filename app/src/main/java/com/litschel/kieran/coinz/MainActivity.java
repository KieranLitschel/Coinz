package com.litschel.kieran.coinz;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
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
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener {

    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private SharedPreferences settings;
    private final String settingsFile = "SettingsFile";
    private boolean justStarted;
    private ArrayList<MarkerOptions> markers;
    private Timer myTimer;
    private Handler myTimerTaskHandler;

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

        settings = getSharedPreferences(settingsFile, Context.MODE_PRIVATE);

        if (justStarted && (!settings.getString("map", "").equals(""))) {
            updateMarkers();
            justStarted = false;
        }

        checkForMapUpdate();
    }

    public void updateMarkers() {
        String mapJSONString = settings.getString("map","");
        map.clear();
        markers = new ArrayList<>();
        if (!mapJSONString.equals("")) {
            try {
                JSONObject mapJSON = new JSONObject(mapJSONString);
                JSONArray markersJSON = mapJSON.getJSONArray("features");
                for (int i = 0; i < markersJSON.length(); i++) {
                    JSONObject marker = markersJSON.getJSONObject(i);
                    JSONArray pos = marker.getJSONObject("geometry").getJSONArray("coordinates");
                    Icon icon = Methods.drawableToIcon(this, R.drawable.marker_icon,
                            Color.parseColor(marker.getJSONObject("properties").getString("marker-color")));
                    markers.add(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(pos.getString(1)),
                                    Double.parseDouble(pos.getString(0))))
                            .title(marker.getJSONObject("properties").getString("id"))
                            .icon(icon));
                }
                map.addMarkers(markers);
                System.out.println("ADDED NEW MARKERS TO MAP");
                System.out.printf("Added %s markers to the map\n", markersJSON.length());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setToUpdateAtMidnight(){
        TimerTask mTt = new TimerTask() {
            public void run() {
                myTimerTaskHandler.post(new Runnable() {
                    public void run(){
                        updateMap();
                        setToUpdateAtMidnight();
                    }
                });
            }
        };
        try {
            myTimer.schedule(mTt,new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
                    .parse(LocalDate.now().plusDays(1).toString()+" 00:00:00"));
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
        new DownloadMapTask(this, map, markers, settings).execute(url);
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
        mapView.onResume();
        checkForMapUpdate();
        myTimer = new Timer();
        myTimerTaskHandler = new Handler();
        setToUpdateAtMidnight();
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

class DownloadMapTask extends AsyncTask<String, Void, String> {
    private SharedPreferences settings;
    private MainActivity mainActivity;
    private MapboxMap map;
    private ArrayList<MarkerOptions> markers;

    DownloadMapTask(MainActivity mainActivity, MapboxMap map, ArrayList<MarkerOptions> markers, SharedPreferences settings) {
        super();
        this.settings = settings;
        this.mainActivity = mainActivity;
        this.map = map;
        this.markers = markers;
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
        DownloadCompleteRunner.downloadMapComplete(mainActivity, map, markers, result, settings);
    }
}

class DownloadCompleteRunner {

    static void downloadMapComplete(MainActivity mainActivity, MapboxMap map, ArrayList<MarkerOptions> markers, String mapJSONString, SharedPreferences settings) {
        if (!(mapJSONString.equals("FAILED"))) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("map", mapJSONString);
            editor.putString("lastDownloadDate", LocalDate.now().toString());
            editor.apply();
            System.out.println("SUCCEEDED IN DOWNLOADING MAP");
            updateMap(mainActivity, map, markers, mapJSONString);
        } else {
            System.out.println("FAILED TO DOWNLOAD MAP");
        }
    }

    static void updateMap(MainActivity mainActivity, MapboxMap map, ArrayList<MarkerOptions> markers, String mapJSONString) {
        markers = new ArrayList<>();
        if (!mapJSONString.equals("")) {
            try {
                JSONObject mapJSON = new JSONObject(mapJSONString);
                JSONArray markersJSON = mapJSON.getJSONArray("features");
                for (int i = 0; i < markersJSON.length(); i++) {
                    JSONObject marker = markersJSON.getJSONObject(i);
                    JSONArray pos = marker.getJSONObject("geometry").getJSONArray("coordinates");
                    Icon icon = Methods.drawableToIcon(mainActivity, R.drawable.marker_icon,
                            Color.parseColor(marker.getJSONObject("properties").getString("marker-color")));
                    markers.add(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(pos.getString(1)),
                                    Double.parseDouble(pos.getString(0))))
                            .title(marker.getJSONObject("properties").getString("id"))
                            .icon(icon));
                }
                map.addMarkers(markers);
                System.out.println("ADDED NEW MARKERS TO MAP");
                System.out.printf("Added %s markers to the map\n", markersJSON.length());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

class Methods{
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
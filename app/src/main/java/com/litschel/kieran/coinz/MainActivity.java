package com.litschel.kieran.coinz;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.litschel.kieran.coinz.NoInternetDialogFragment.NoInternetDialogCallback;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.StampedLock;

public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener, MapUpdateCallback, MapDownloadedCallback, CoinsUpdatedCallback, NoInternetDialogCallback {

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
    private StampedLock mapUpdateLock = new StampedLock();
    private ExecutorService mapUpdateExecutor;
    private DrawerLayout mDrawerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        justStarted = true;
        // Need lock to prevent map being written before it is created
        long lockStamp = mapUpdateLock.writeLock();
        mapUpdateExecutor = Executors.newFixedThreadPool(1);

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

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            mapUpdateLock.unlockWrite(lockStamp);
            enableLocationPlugin();
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        settings = getSharedPreferences(settingsFile, Context.MODE_PRIVATE);
        uid = settings.getString("uid", "");

        System.out.println("GOT UID OF " + uid + " FROM LOCAL STORAGE");

        db = FirebaseFirestore.getInstance();

        signIn();

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

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
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

        if (justStarted && (!settings.getString("map", "").equals(""))) {
            System.out.println("INITIALIZE LOCATION ENGINE WAITING FOR LOCK");
            long lockStamp = mapUpdateLock.writeLock();
            System.out.println("INITIALIZE LOCATION ENGINE ACQUIRED LOCK");
            updateMarkers(lockStamp);
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
                    if (ThirdPartyMethods.distance(latitude, longitude,
                            markerPos.getLatitude(), markerPos.getLongitude(), "K") * 1000 <= 25) {
                        markersToRemove.add(marker);
                    }
                }
                if (markersToRemove.size() > 0) {
                    removeMarkers(markersToRemove);
                }
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

        // Update coins in a seperate thread so we can use thread locks to prevent concurrent updates
        // to the database and JSONObject
        // Use an executor to avoid having to create new threads which is expensive
        mapUpdateExecutor.submit(new CoinsUpdateTask(this, mapUpdateLock, db, uid, markersToRemove, settings));
    }

    private void setToUpdateAtMidnight() {
        TimerTask mTt = new TimerTask() {
            public void run() {
                myTimerTaskHandler.post(new Runnable() {
                    public void run() {
                        checkForMapUpdate();
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
            System.out.println("CHECKING IF MAP UPDATE REQUIRED");

            // Not the ideal way to clear the map as may be executed multiple times, but there was a strange bug with mapbox where
            // if I tried to remove a marker from the map on another thread the thread would just enter the method and never return
            // from it, without even throwing an exception. Despite clearling the map outside the thread, it is necessary to delete
            // the markers on a seperate thread to avoid intefering with collecting them

            LocalDate lastDownloadDate = LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString()));
            if (lastDownloadDate.isBefore(LocalDate.now()) && map != null) {
                System.out.println("CLEARING MAP");
                map.clear();
                System.out.println("CLEARED MAP!");
            }

            mapUpdateExecutor.submit(new MapUpdateTask(this, mapUpdateLock, settings));
        }
    }

    @Override
    public void updateMap(long lockStamp) {
        if (!settings.getString("map","").equals("")){
            markers = new ArrayList<>();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("map", "");
            editor.apply();
        }
        LocalDate today = LocalDate.now();
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
        new DownloadMapTask(this, mapUpdateLock, lockStamp).execute(url);
    }

    private void setToUpdateOnInternet() {
        System.out.println("CREATED TIMER TASK");
        TimerTask mTtInternet = new TimerTask() {
            public void run() {
                myTimerTaskHandler.post(() -> {
                    System.out.println("TIMER TASK TRIGGERED");
                    if (isNetworkAvailable()) {
                        System.out.println("TIMER TASK FOUND INTERNET");
                        checkForMapUpdate();
                    } else {
                        System.out.println("TIMER TASK FOUND NO INTERNET");
                        setToUpdateOnInternet();
                    }
                });
            }
        };
        System.out.println("TIMER TASK SCHEDULED");
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
        if (mapUpdateExecutor.isShutdown()){
            mapUpdateExecutor = Executors.newFixedThreadPool(1);
        }
        mapView.onStart();
        myTimer = new Timer();
        myTimerTaskHandler = new Handler();
        setToUpdateAtMidnight();
        checkForMapUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    private void signIn() {
        if (uid.equals("")){
            if (isNetworkAvailable()){
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build());

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            } else {
                DialogFragment noInternetFragment = new NoInternetDialogFragment();
                noInternetFragment.setCancelable(false);
                noInternetFragment.show(getSupportFragmentManager(), "no_internet_dialog");
            }
        }
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

    private void checkFirstTimeUser() {
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

    private void setFirstTimeUser() {
        Map<String, Object> user_defaults = new HashMap<>();
        user_defaults.put("DOLR", 0.0);
        user_defaults.put("GOLD", 0.0);
        user_defaults.put("PENY", 0.0);
        user_defaults.put("QUID", 0.0);
        user_defaults.put("SHIL", 0.0);
        db.collection("users").document(uid)
                .set(user_defaults)
                .addOnSuccessListener(aVoid -> System.out.println("SUCCESSFULLY ADDED USER TO DATABASE"))
                .addOnFailureListener(e -> System.out.println("FAILED TO ADD USER TO DATABASE"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
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
        myTimer.cancel();
        myTimer.purge();
        mapUpdateExecutor.shutdown();
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
        mapUpdateExecutor.shutdownNow();
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
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logout() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("uid", "");
        editor.apply();
        uid = "";
        signIn();
    }

    @Override
    public void onMapDownloaded(String mapJSONString, long lockStamp) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("map", mapJSONString);
        editor.putString("lastDownloadDate", LocalDate.now().toString());
        editor.apply();
        System.out.println("SUCCEEDED IN DOWNLOADING MAP");
        updateMarkers(mapJSONString, lockStamp);
    }

    public void updateMarkers(long lockStamp) {
        String mapJSONString = settings.getString("map", "");
        map.clear();
        updateMarkers(mapJSONString, lockStamp);
    }

    public void updateMarkers(String mapJSONString, long lockStamp) {
        markers = new ArrayList<>();
        if (!mapJSONString.equals("")) {
            try {
                JSONObject mapJSON = new JSONObject(mapJSONString);
                JSONArray markersJSON = mapJSON.getJSONArray("features");
                for (int i = 0; i < markersJSON.length(); i++) {
                    JSONObject markerJSON = markersJSON.getJSONObject(i);
                    JSONArray pos = markerJSON.getJSONObject("geometry").getJSONArray("coordinates");
                    Icon icon = ThirdPartyMethods.drawableToIcon(this, R.drawable.marker_icon,
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
        mapUpdateLock.unlockWrite(lockStamp);
        System.out.println("UPDATE MARKERS RELEASED LOCK");
    }

    @Override
    public void onCoinsUpdated(long lockStamp, HashMap<Marker, String[]> markerDetails, JSONObject mapJSON) {
        ArrayList<String[]> coinsCollected = new ArrayList<>();
        for (Marker marker : markerDetails.keySet()) {
            map.removeMarker(marker);
            markers.remove(marker);
            coinsCollected.add(markerDetails.get(marker));
            System.out.println("Removed marker " + marker.getTitle());
        }
        if (coinsCollected.size() == 1) {
            String currency = coinsCollected.get(0)[0];
            String value = coinsCollected.get(0)[1];
            Snackbar.make(findViewById(R.id.toolbar), "Collected " + value
                    + " " + currency, Snackbar.LENGTH_LONG).show();
        } else {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Collected ");
            for (int i = 0; i < coinsCollected.size() - 1; i++) {
                String currency = coinsCollected.get(i)[0];
                String value = coinsCollected.get(i)[1];
                messageBuilder.append(value);
                messageBuilder.append(" ");
                messageBuilder.append(currency);
                // Typically we only add the comma when we are listing them, which will only occur if
                // there is more than 2 coins collected at once
                if (coinsCollected.size() > 2) {
                    messageBuilder.append(", ");
                } else {
                    messageBuilder.append(" ");
                }
            }
            String currency = coinsCollected.get(coinsCollected.size() - 1)[0];
            String value = coinsCollected.get(coinsCollected.size() - 1)[1];
            messageBuilder.append("and ");
            messageBuilder.append(value);
            messageBuilder.append(" ");
            messageBuilder.append(currency);
            Snackbar.make(findViewById(R.id.toolbar), messageBuilder.toString(), Snackbar.LENGTH_LONG).show();
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("map", mapJSON.toString());
        editor.apply();
        mapUpdateLock.unlockWrite(lockStamp);
        System.out.println("ON COINS UPDATED RELEASED LOCK");
    }

    @Override
    public void tryInternetAgain() {
        signIn();
    }

    @Override
    public void closeApp() {
        finish();
        System.exit(0);
    }
}
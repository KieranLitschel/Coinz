package com.litschel.kieran.coinz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.StampedLock;

public class MapFragment extends Fragment implements LocationEngineListener, PermissionsListener, MapUpdateCallback, MapDownloadedCallback, RemoveMarkersCallback {

    private Context activity;
    private MainActivity mainActivity;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private MapView mapView;
    private MapboxMap map;
    private ExecutorService mapUpdateExecutor;
    private StampedLock settingsWriteLock;
    private SharedPreferences settings;
    private FirebaseFirestore db;
    private ArrayList<MarkerOptions> markerOpts;
    private Timer myTimer;
    private Handler myTimerTaskHandler;
    private boolean justCreated;
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private String users;
    private HashMap<MarkerOptions, String> markerIds;

    // We use this to get values from MainActivity we'll need later
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context;
        if (getActivity() != null) {
            if (((MainActivity) getActivity()).getClass() == MainActivity.class) {
                mainActivity = ((MainActivity) getActivity());
                settings = mainActivity.settings;
            } else {
                System.out.println("ACTIVITY CLASS WAS EXPECTED TO BE MAIN ACTIVITY BUT ISN'T");
            }
        } else {
            System.out.println("ACTIVITY WAS NULL WHEN EXPECTED NON-NULL");
        }
        db = mainActivity.db;
        settingsWriteLock = mainActivity.settingsWriteLock;
        users = mainActivity.users;
    }

    // Set the fragment to the map
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        justCreated = true;
        super.onViewCreated(view, savedInstanceState);

        // Create the mapUpdateExecutor, we use an executor for the same reason we did for the coinsUpdateExecutor.
        // We use a seperate executor so that it is destroyed when the MapFragment is, as the callbacks from the threads
        // interact with the map

        mapUpdateExecutor = Executors.newFixedThreadPool(1);

        // Create the map

        Mapbox.getInstance(activity, getString(R.string.mapbox_access_token));

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            // Fix the bounds of play
            LatLngBounds PLAY_BOUNDS = new LatLngBounds.Builder()
                    .include(new LatLng(55.946233, -3.192473))
                    .include(new LatLng(55.946233, -3.184319))
                    .include(new LatLng(55.942617, -3.192473))
                    .include(new LatLng(55.942617, -3.184319))
                    .build();
            map.setLatLngBoundsForCameraTarget(PLAY_BOUNDS);
            enableLocationPlugin();
            // Run initial setup after the map has been created to avoid null pointer exception on map
            initialSetup();
        });

        // Create the FAB to reset the view
        FloatingActionButton zoomOutFAB = view.findViewById(R.id.zoomOutFAB);
        zoomOutFAB.setOnClickListener(view1 -> {
            CameraPosition position = new CameraPosition.Builder()
                    .target(new LatLng(55.944425, -3.188396))
                    .zoom(15)
                    .bearing(0)
                    .tilt(0)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
        });

        // For testers we add an FAB that collects the first coin in the array of coins, this allows
        // spoofing collecting coins, with the only difference being the location listener isn't fired
        // to trigger the response. I do this as I found mapBox was bugging in the emulator, so I could
        // not guarentee the location listener would produce repeatable tests, so opted to circumvent it
        FloatingActionButton collectCoinFAB = view.findViewById(R.id.collectCoinFAB);
        if (users.equals("users-test")) {
            collectCoinFAB.setVisibility(View.VISIBLE);
            collectCoinFAB.setEnabled(true);
            collectCoinFAB.setOnClickListener(view12 -> {
                ArrayList<MarkerOptions> markersToRemove = new ArrayList<>();
                markersToRemove.add(markerOpts.get(0));
                removeMarkers(markersToRemove);
            });
        } else {
            collectCoinFAB.setVisibility(View.GONE);
            collectCoinFAB.setEnabled(false);
        }
    }

    private void initialSetup() {
        // We set the map to update at midnight
        myTimer = new Timer();
        myTimerTaskHandler = new Handler();
        setToUpdateAtMidnight();
        if (settings.getString("lastDownloadDate", "").equals("")) {

            // If the value for lastDownloadDate is an empty string, we haven't downloaded the database from firebase
            // so we do so

            if (mainActivity.isNetworkAvailable()) {
                initialMapSetup();
            } else {

                // If there is no internet we will not be able to contact firebase, so we schedule to run the method when there is internet

                Toast.makeText(activity, "Will update map when there is an internet connection", Toast.LENGTH_LONG)
                        .show();
                mainActivity.waitingToInitialMapSetup = true;
            }
        } else {

            // If it's not empty then we have local stored data, so we load the locally stored markers
            // onto the map and check for a map update

            System.out.println("ON START WAITING FOR LOCK");
            long lockStamp = settingsWriteLock.writeLock();
            System.out.println("ON START ACQUIRED LOCK");
            updateMarkers(lockStamp);
            checkForMapUpdate();
        }
    }

    // Setup the location listener so that we can check when the users moves if they are within distance of a coin
    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            // We check markerOpts are not null in case the user moves but there is no markers, which
            // for example can occur when we cleared the map at midnight but the user had no internet
            if (markerOpts != null) {
                ArrayList<MarkerOptions> markersToRemove = new ArrayList<>();
                for (MarkerOptions markerOpt : markerOpts) {
                    Marker marker = markerOpt.getMarker();
                    LatLng markerPos = marker.getPosition();
                    // If the user in a 25m radius of a marker, we mark it for collection (removal)
                    if (ThirdPartyMethods.distance(latitude, longitude,
                            markerPos.getLatitude(), markerPos.getLongitude()) * 1000 <= 25) {
                        markersToRemove.add(markerOpt);
                    }
                }
                // If the user is in the radius of at least one marker we begin the process of removing it
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
    private void removeMarkers(ArrayList<MarkerOptions> markersToRemove) {

        // Update coins in a seperate thread so we can use thread locks to prevent concurrent updates
        // to the database and JSONObject
        // Use an executor to avoid having to create new threads which is expensive
        mapUpdateExecutor.submit(new RemoveMarkersTask(this, markerIds, settingsWriteLock, mainActivity, db, mainActivity.uid, markersToRemove, settings));
    }

    // This method updates the map at midnight

    // Suppressed warning on LocalDateFormat as game is based in Edinburgh, so don't expect
    // it to be used internationally
    @SuppressLint("SimpleDateFormat")
    private void setToUpdateAtMidnight() {
        // We disable it for testers, as spoofing the date means it does not behave as expected
        if (!mainActivity.tester) {
            TimerTask mTt = new TimerTask() {
                public void run() {
                    myTimerTaskHandler.post(() -> {
                        // At midnight we check for a map update, and set it to update at midnight
                        // the following day
                        checkForMapUpdate();
                        setToUpdateAtMidnight();
                    });
                }
            };
            // Schedule the task to be executed at midnight
            try {
                myTimer.schedule(mTt, new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
                        .parse(LocalDate.now().plusDays(1).toString() + " 00:00:00"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // Checks for a map update via the mapUpdateExecutor

    public void checkForMapUpdate() {
        if (settings != null) {
            System.out.println("CHECKING IF MAP UPDATE REQUIRED");
            // We run map updates on a seperate thread as it will have to modify the settings, so we
            // will need to acquire the settingsWriteLock to prevent errors from concurrent writes
            mapUpdateExecutor.submit(new MapUpdateTask(mainActivity, this, settingsWriteLock, settings));
        }
    }

    // This is the callback from the mapUpdateTask which updates the map

    @Override
    public void updateMap(long lockStamp) {
        // If the map isn't already empty, we delete it, and ensure the markers are cleared so no markers
        // from yesterday can be collected before the map updates
        if (!settings.getString("map", "").equals("")) {
            markerOpts = new ArrayList<>();
            // This will be called on a thread seperate to the main activity, so we must tell it to run on the UI thread
            mainActivity.runOnUiThread(() -> map.clear());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("map", "");
            editor.apply();
        }
        if (!mainActivity.isNetworkAvailable()) {
            // If the network isn't available we inform the user it'll be downloaded when they connect to the internet,
            // and set it so when the network connection changes next the map will be updated
            mainActivity.waitingToUpdateMap = true;
            settingsWriteLock.unlockWrite(lockStamp);
            // Similarly to for map.clear, as this changes a UI element we must call it on the UI thread
            mainActivity.runOnUiThread(() -> Toast.makeText(activity, "Will update map when there is an internet connection", Toast.LENGTH_LONG)
                    .show());
        } else {
            // Use the date today to workout what URL we need to download todays map from
            LocalDate today = mainActivity.localDateNow();
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
            // If the network is avaialable we start the async task to download the map
            new DownloadMapTask(this, settingsWriteLock, lockStamp).execute(url);
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        super.onStart();
        // Set up navigation as specified in the mapbox navigation tutorial
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
        if (mapUpdateExecutor.isShutdown()) {
            mapUpdateExecutor = Executors.newFixedThreadPool(1);
        }
        // initialSetup is run in onCreateView when the view is created instead of here in order to
        // prevent mapUpdate being called before the map is created
        if (!justCreated) {
            initialSetup();
        } else {
            justCreated = false;
        }
    }

    // Performs the initial setup of the map including getting values in the database that need
    // to be stored locally for offline play
    public void initialMapSetup() {
        DocumentReference docRef = db.collection(users).document(mainActivity.uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // The following code just gets values from the database and then puts them in
                    // the settings file, whilst also avoiding potentiall null pointer exceptions
                    String username = document.getString("username");
                    String mapJSONString = document.getString("map");
                    if (mapJSONString == null) {
                        System.out.println("USERS DOCUMENT DOES NOT CONTAIN FIELD FOR MAP");
                        mapJSONString = "";
                    }
                    String lastDownloadedDate = document.getString("lastDownloadDate");
                    Double coinsRemainingTodayUnchecked = document.getDouble("coinsRemainingToday");
                    double coinsRemainingToday;
                    if (coinsRemainingTodayUnchecked != null) {
                        coinsRemainingToday = coinsRemainingTodayUnchecked;
                    } else {
                        coinsRemainingToday = 0.0;
                        System.out.println("USERS DOCUMENT DOES NOT CONTAIN FIELD FOR COINSREMAININGTODAY");
                    }
                    HashMap<String, String> amountOfCurrencies = new HashMap<>();
                    for (String currency : currencies) {
                        Double amountOfCurrency = document.getDouble(currency);
                        if (amountOfCurrency != null) {
                            amountOfCurrencies.put(currency, Double.toString(amountOfCurrency));
                        } else {
                            System.out.println("USERS DOCUMENT DOES NOT CONTAIN FIELD FOR " + currency);
                            amountOfCurrencies.put(currency, "0");
                        }
                    }
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("username", username);
                    editor.putString("map", mapJSONString);
                    editor.putString("lastDownloadDate", lastDownloadedDate);
                    editor.putString("coinsRemainingToday", Double.toString(coinsRemainingToday));
                    for (String currency : currencies) {
                        editor.putString(currency, amountOfCurrencies.get(currency));
                    }
                    editor.apply();
                    // If there was a map in the database, show it to the user
                    if (!mapJSONString.equals("")) {
                        System.out.println("ON START WAITING FOR LOCK");
                        long lockStamp = settingsWriteLock.writeLock();
                        System.out.println("ON START ACQUIRED LOCK");
                        updateMarkers(lockStamp);
                    }
                    checkForMapUpdate();
                } else {
                    System.out.println("COULDN'T FIND USER IN FIREBASE");
                }
            } else {
                System.out.printf("GETTING DOCUMENT FAILED WITH EXCEPTION: %s\n", task.getException());
            }
        });
    }

    // This callback is called once the DownloadMapTask completes

    @Override
    public void onMapDownloaded(String mapJSONString, long lockStamp) {
        System.out.println("SUCCEEDED IN DOWNLOADING MAP");
        // Put the new downloaded map in the database
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("lastDownloadDate", mainActivity.localDateNow().toString());
        mapData.put("map", mapJSONString);
        // It's a new day, so set the coinsRemainingToday to 25
        mapData.put("coinsRemainingToday", 25);
        db.collection(users).document(mainActivity.uid)
                .update(mapData)
                .addOnSuccessListener(aVoid -> {
                    // Once we've put the map in the database, update local values to reflect the change
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("map", mapJSONString);
                    editor.putString("lastDownloadDate", mainActivity.localDateNow().toString());
                    editor.putString("coinsRemainingToday","25");
                    editor.apply();
                    System.out.println("UPDATED MAP IN FIREBASE SUCCESSFULLY");
                    updateMarkers(mapJSONString, lockStamp);
                })
                .addOnFailureListener(e -> {
                    System.out.printf("FAILED TO UPDATE MAP IN FIREBASE WITH EXCEPTION: %s\n", e.getMessage());
                    settingsWriteLock.unlockWrite(lockStamp);
                    System.out.println("ON MAP DOWNLOADED RELEASED LOCK");
                });
    }

    // This overload of updateMarkers differs from the other as it is called from within MapFragment,
    // whereas the other is called in the callback from DownloadMapTask, so we need to acquire a lock
    // to use it

    public void updateMarkers(long lockStamp) {
        String mapJSONString = settings.getString("map", "");
        // We clear the markers from the map
        map.clear();
        updateMarkers(mapJSONString, lockStamp);
    }

    // Updates the markers in the map, still retain the lock until after it completes to prevent
    // concurrent writes markerIds and markerOpts, which could cause strange behaviour

    public void updateMarkers(String mapJSONString, long lockStamp) {
        // Clear the markers
        markerOpts = new ArrayList<>();
        markerIds = new HashMap<>();
        if (!mapJSONString.equals("")) {
            try {
                // Parse the map as a JSON so we can read it simpler
                JSONObject mapJSON = new JSONObject(mapJSONString);
                // Read the "features" (markers) as a JSONArray, so we can iterate through it
                JSONArray markersJSON = mapJSON.getJSONArray("features");
                for (int i = 0; i < markersJSON.length(); i++) {
                    JSONObject markerJSON = markersJSON.getJSONObject(i);
                    JSONArray pos = markerJSON.getJSONObject("geometry").getJSONArray("coordinates");
                    // Colour the marker as is specified for it in JSON
                    Icon icon = ThirdPartyMethods.drawableToIcon(activity,
                            Color.parseColor(markerJSON.getJSONObject("properties").getString("marker-color")));
                    String value = markerJSON.getJSONObject("properties").getString("value");
                    String currency = markerJSON.getJSONObject("properties").getString("currency");
                    // Create the marker option with its position, icon, and the annotation of the markers
                    // value and currency
                    MarkerOptions markerOption = new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(pos.getString(1)),
                                    Double.parseDouble(pos.getString(0))))
                            .title(value + " " + currency)
                            .icon(icon);
                    // Store the ID of the marker in a hashmap so that we can query the id of the marker via
                    // it's markerOpt when we want to remove it
                    markerIds.put(markerOption, markerJSON.getJSONObject("properties").getString("id"));
                    // Keep a record of the markerOpts so we can identify the markerOpt that corresponds
                    // to each marker when we want to collect the coins
                    markerOpts.add(markerOption);
                    // Add the marker to the map
                    map.addMarker(markerOption);
                }
                System.out.println("ADDED NEW MARKERS TO MAP");
                System.out.printf("Added %s markers to the map\n", markersJSON.length());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        settingsWriteLock.unlockWrite(lockStamp);
        System.out.println("UPDATE MARKERS RELEASED LOCK");
    }

    // This is the callback for the RemoveMarkersTask

    @Override
    public void onMarkerRemoved(long lockStamp, HashMap<MarkerOptions, String[]> markerDetails, JSONObject mapJSON) {
        ArrayList<String[]> coinsCollected = new ArrayList<>();
        // We extract from the marker details what markers have been removed, remove them from the map
        // and collects within the fragment, and keep a record of their details
        for (MarkerOptions markerOpt : markerDetails.keySet()) {
            // We use runOnUiThread here as this method is called from outside the UI thread
            Marker marker = markerOpt.getMarker();
            mainActivity.runOnUiThread(() -> map.removeMarker(marker));
            markerOpts.remove(markerOpt);
            coinsCollected.add(markerDetails.get(markerOpt));
            System.out.println("Removed marker " + markerIds.get(markerOpt));
            markerIds.remove(markerOpt);
        }
        // We display the details of the coins collected via a toast, we use an if statement here to control the
        // formatting of the message, as when there are more than 1 coin we use "and" and ","
        if (coinsCollected.size() == 1) {
            String currency = coinsCollected.get(0)[0];
            String value = coinsCollected.get(0)[1];
            mainActivity.runOnUiThread(() -> Toast.makeText(activity, "Collected " + value
                    + " " + currency, Toast.LENGTH_LONG).show());
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
            mainActivity.runOnUiThread(() -> Toast.makeText(activity, messageBuilder.toString(), Toast.LENGTH_LONG).show());
        }
        settingsWriteLock.unlockWrite(lockStamp);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
        // We cancel and purge the timer, and shuttdown the mapUpdateExecutor on stop to save resources
        myTimer.cancel();
        myTimer.purge();
        mapUpdateExecutor.shutdown();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
        mapUpdateExecutor.shutdownNow();
    }

    // The final methods relate to finding the users location, these are mostly based on the originals
    // show in the mapbox navigation tutorial https://www.mapbox.com/help/android-navigation-sdk/

    // Set up permissions
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
            locationPlugin.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(mainActivity);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(activity);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation == null) {
            locationEngine.addLocationEngineListener(this);
        }

        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    2000,
                    10, locationListenerGPS);
        } else {
            System.out.println("EXPECTED LOCATION MANAGER TO BE NON-NULL BUT WAS NULL");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(mainActivity, getString(R.string.user_location_permission_explanation), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            mainActivity.finish();
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
}

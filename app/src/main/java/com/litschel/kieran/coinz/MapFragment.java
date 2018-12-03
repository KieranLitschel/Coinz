package com.litschel.kieran.coinz;

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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Comparator;
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
    private LocationManager locationManager;
    private MapView mapView;
    private MapboxMap map;
    private ExecutorService mapUpdateExecutor;
    private StampedLock mapUpdateLock;
    private SharedPreferences settings;
    private FirebaseFirestore db;
    private ArrayList<MarkerOptions> markerOpts;
    private Timer myTimer;
    private Handler myTimerTaskHandler;
    private boolean justCreated;
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private String users;
    private HashMap<MarkerOptions, String> markerIds;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context;
        mainActivity = ((MainActivity) getActivity());
        settings = ((MainActivity) getActivity()).settings;
        db = ((MainActivity) getActivity()).db;
        mapUpdateLock = ((MainActivity) getActivity()).mapUpdateLock;
        users = ((MainActivity) getActivity()).users;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        justCreated = true;
        super.onViewCreated(view, savedInstanceState);

        mapUpdateExecutor = Executors.newFixedThreadPool(1);

        Mapbox.getInstance(activity, getString(R.string.mapbox_access_token));

        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            enableLocationPlugin();
            // Run initial setup after the map has been created to avoid null pointer exception on map
            initialSetup();
        });

        FloatingActionButton zoomOutFAB = (FloatingActionButton) view.findViewById(R.id.zoomOutFAB);
        zoomOutFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(55.944425, -3.188396))
                        .zoom(15)
                        .bearing(0)
                        .tilt(0)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
            }
        });

        FloatingActionButton collectCoinFAB = (FloatingActionButton) view.findViewById(R.id.collectCoinFAB);
        if (users.equals("users-test")){
            collectCoinFAB.setVisibility(View.VISIBLE);
            collectCoinFAB.setEnabled(true);
            collectCoinFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<MarkerOptions> markersToRemove = new ArrayList<>();
                    markersToRemove.add(markerOpts.get(0));
                    removeMarkers(markersToRemove);
                }
            });
        } else {
            collectCoinFAB.setVisibility(View.GONE);
            collectCoinFAB.setEnabled(false);
        }
    }

    private void initialSetup() {
        myTimer = new Timer();
        myTimerTaskHandler = new Handler();
        setToUpdateAtMidnight();
        if (settings.getString("map", "").equals("")) {
            // If there is no internet we will not be able to contact firebase, so we schedule to run the method when there is internet
            if (((MainActivity) getActivity()).isNetworkAvailable()) {
                initialMapSetup();
            } else {
                Toast.makeText(activity, "Will update map when there is an internet connection", Toast.LENGTH_LONG)
                        .show();
                ((MainActivity) getActivity()).waitingToInitialMapSetup = true;
            }
        } else {
            System.out.println("ON START WAITING FOR LOCK");
            long lockStamp = mapUpdateLock.writeLock();
            System.out.println("ON START ACQUIRED LOCK");
            updateMarkers(lockStamp);
            checkForMapUpdate();
        }
    }

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
            permissionsManager.requestLocationPermissions(getActivity());
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

        LatLngBounds PLAY_BOUNDS = new LatLngBounds.Builder()
                .include(new LatLng(55.946233, -3.192473))
                .include(new LatLng(55.946233, -3.184319))
                .include(new LatLng(55.942617, -3.192473))
                .include(new LatLng(55.942617, -3.184319))
                .build();
        map.setLatLngBoundsForCameraTarget(PLAY_BOUNDS);

        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000,
                10, locationListenerGPS);
    }

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            if (markerOpts != null) {
                ArrayList<MarkerOptions> markersToRemove = new ArrayList<>();
                for (MarkerOptions markerOpt : markerOpts) {
                    Marker marker = markerOpt.getMarker();
                    LatLng markerPos = marker.getPosition();
                    if (ThirdPartyMethods.distance(latitude, longitude,
                            markerPos.getLatitude(), markerPos.getLongitude(), "K") * 1000 <= 25) {
                        markersToRemove.add(markerOpt);
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
    private void removeMarkers(ArrayList<MarkerOptions> markersToRemove) {

        // Update coins in a seperate thread so we can use thread locks to prevent concurrent updates
        // to the database and JSONObject
        // Use an executor to avoid having to create new threads which is expensive
        mapUpdateExecutor.submit(new RemoveMarkersTask(this, markerIds, mapUpdateLock, ((MainActivity) getActivity()), db, ((MainActivity) getActivity()).uid, markersToRemove, settings));
    }

    private void setToUpdateAtMidnight() {
        if (!((MainActivity) getActivity()).tester){
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
    }

    public void checkForMapUpdate() {
        if (settings != null) {
            System.out.println("CHECKING IF MAP UPDATE REQUIRED");
            mapUpdateExecutor.submit(new MapUpdateTask(((MainActivity) getActivity()),this, mapUpdateLock, settings));
        }
    }

    @Override
    public void updateMap(long lockStamp) {
        if (!settings.getString("map", "").equals("")) {
            markerOpts = new ArrayList<>();
            // This will be called on a thread seperate to the main activity, so we must tell it to run on the UI thread
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    map.clear();
                }
            });
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("map", "");
            editor.apply();
        }
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
        if (!((MainActivity) getActivity()).isNetworkAvailable()) {
            ((MainActivity) getActivity()).waitingToUpdateMap = true;
            mapUpdateLock.unlockWrite(lockStamp);
            // Similarly to for map.clear, as this changes a UI element we must call it on the UI thread
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Will update map when there is an internet connection", Toast.LENGTH_LONG)
                            .show();
                }
            });
        } else {
            new DownloadMapTask(this, mapUpdateLock, lockStamp).execute(url);
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
            ((MainActivity) getActivity()).finish();
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
    public void onStart() {
        super.onStart();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        if (mapUpdateExecutor.isShutdown()) {
            mapUpdateExecutor = Executors.newFixedThreadPool(1);
        }
        mapView.onStart();
        // initialSetup is run in onCreateView when the view is created instead of here in order to
        // prevent mapUpdate being called before the map is created
        if (!justCreated) {
            initialSetup();
        } else {
            justCreated = false;
        }
    }

    public void initialMapSetup() {
        DocumentReference docRef = db.collection(users).document(((MainActivity) getActivity()).uid);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String username = document.getString("username");
                        String mapJSONString = document.getString("map");
                        String lastDownloadedDate = document.getString("lastDownloadDate");
                        double coinsRemainingToday = document.getDouble("coinsRemainingToday");
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("username", username);
                        editor.putString("map", mapJSONString);
                        editor.putString("lastDownloadDate", lastDownloadedDate);
                        editor.putString("coinsRemainingToday", Double.toString(coinsRemainingToday));
                        for (String currency : currencies) {
                            editor.putString(currency, Double.toString(document.getDouble(currency)));
                        }
                        editor.apply();
                        if (!settings.getString("map", "").equals("")) {
                            System.out.println("ON START WAITING FOR LOCK");
                            long lockStamp = mapUpdateLock.writeLock();
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
            }
        });
    }


    @Override
    public void onMapDownloaded(String mapJSONString, long lockStamp) {
        System.out.println("SUCCEEDED IN DOWNLOADING MAP");
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("lastDownloadDate", mainActivity.localDateNow().toString());
        mapData.put("map", mapJSONString);
        LocalDate lostConnectionDate = LocalDate.parse(settings.getString("lostConnectionDate", LocalDate.MIN.toString()));
        double coinsRemainingTodayDelta;
        if (lostConnectionDate.isEqual(mainActivity.localDateNow())) {
            coinsRemainingTodayDelta = Double.parseDouble(settings.getString("coinsRemainingTodayDelta", "0"));
        } else {
            coinsRemainingTodayDelta = 0;
        }
        mapData.put("coinsRemainingToday", 25 - coinsRemainingTodayDelta);
        db.collection(users).document(((MainActivity) getActivity()).uid)
                .update(mapData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("map", mapJSONString);
                        editor.putString("lastDownloadDate", mainActivity.localDateNow().toString());
                        editor.putString("coinsRemainingToday", Double.toString(25 - coinsRemainingTodayDelta));
                        editor.remove("lostConnectionDate");
                        editor.remove("coinsRemainingTodayDelta");
                        editor.apply();
                        System.out.println("UPDATED MAP IN FIREBASE SUCCESSFULLY");
                        updateMarkers(mapJSONString, lockStamp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.printf("FAILED TO UPDATE MAP IN FIREBASE WITH EXCEPTION: %s\n", e.getMessage());
                        mapUpdateLock.unlockWrite(lockStamp);
                        System.out.println("ON MAP DOWNLOADED RELEASED LOCK");
                    }
                });
    }

    public void updateMarkers(long lockStamp) {
        String mapJSONString = settings.getString("map", "");
        map.clear();
        updateMarkers(mapJSONString, lockStamp);
    }

    public void updateMarkers(String mapJSONString, long lockStamp) {
        markerOpts = new ArrayList<>();
        markerIds = new HashMap<>();
        if (!mapJSONString.equals("")) {
            try {
                JSONObject mapJSON = new JSONObject(mapJSONString);
                JSONArray markersJSON = mapJSON.getJSONArray("features");
                for (int i = 0; i < markersJSON.length(); i++) {
                    JSONObject markerJSON = markersJSON.getJSONObject(i);
                    JSONArray pos = markerJSON.getJSONObject("geometry").getJSONArray("coordinates");
                    Icon icon = ThirdPartyMethods.drawableToIcon(activity, R.drawable.marker_icon,
                            Color.parseColor(markerJSON.getJSONObject("properties").getString("marker-color")));
                    String value = markerJSON.getJSONObject("properties").getString("value");
                    String currency = markerJSON.getJSONObject("properties").getString("currency");
                    MarkerOptions markerOption = new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(pos.getString(1)),
                                    Double.parseDouble(pos.getString(0))))
                            .title(value+" "+currency)
                            .icon(icon);
                    markerIds.put(markerOption, markerJSON.getJSONObject("properties").getString("id"));
                    markerOpts.add(markerOption);
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
    public void onCoinsUpdated(long lockStamp, HashMap<MarkerOptions, String[]> markerDetails, JSONObject mapJSON) {
        ArrayList<String[]> coinsCollected = new ArrayList<>();
        for (MarkerOptions markerOpt : markerDetails.keySet()) {
            // We use runOnUiThread here as this method is called from outside the UI thread
            Marker marker = markerOpt.getMarker();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    map.removeMarker(marker);
                }
            });
            markerOpts.remove(markerOpt);
            coinsCollected.add(markerDetails.get(markerOpt));
            System.out.println("Removed marker " + markerIds.get(markerOpt));
            markerIds.remove(markerOpt);
        }
        if (coinsCollected.size() == 1) {
            String currency = coinsCollected.get(0)[0];
            String value = coinsCollected.get(0)[1];
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Collected " + value
                            + " " + currency, Toast.LENGTH_LONG).show();
                }
            });
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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, messageBuilder.toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
        String mapJSONString = mapJSON.toString();
        if (((MainActivity) getActivity()).isNetworkAvailable()) {
            Map<String, Object> mapData = new HashMap<>();
            mapData.put("map", mapJSONString);
            db.collection(users).document(((MainActivity) getActivity()).uid)
                    .update(mapData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString("map", mapJSONString);
                            editor.apply();
                            mapUpdateLock.unlockWrite(lockStamp);
                            System.out.println("ON COINS UPDATED RELEASED LOCK");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            System.out.printf("FAILED TO UPDATE MAP IN FIREBASE WITH EXCEPTION: %s\n", e);
                            mapUpdateLock.unlockWrite(lockStamp);
                            System.out.println("ON COINS UPDATED RELEASED LOCK");
                        }
                    });
        } else {
            // If offline we only update the map locally
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("map", mapJSONString);
            editor.apply();
            mapUpdateLock.unlockWrite(lockStamp);
            System.out.println("ON COINS UPDATED RELEASED LOCK");
        }
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
        myTimer.cancel();
        myTimer.purge();
        mapUpdateExecutor.shutdown();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
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
}

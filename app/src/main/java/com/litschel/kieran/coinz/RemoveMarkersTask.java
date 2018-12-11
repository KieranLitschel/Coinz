package com.litschel.kieran.coinz;


import android.content.SharedPreferences;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

// This task removes markers from the map

public class RemoveMarkersTask implements Runnable {
    private RemoveMarkersCallback context;
    private StampedLock settingsWriteLock;
    private FirebaseFirestore db;
    private String uid;
    private ArrayList<MarkerOptions> markersToRemove;
    private SharedPreferences settings;
    private MainActivity activity;
    private String users;
    private HashMap<MarkerOptions, String> markerIds;

    RemoveMarkersTask(RemoveMarkersCallback context, HashMap<MarkerOptions, String> markerIds, StampedLock settingsWriteLock, MainActivity activity, FirebaseFirestore db, String uid, ArrayList<MarkerOptions> markersToRemove, SharedPreferences settings) {
        super();
        this.context = context;
        this.settingsWriteLock = settingsWriteLock;
        this.db = db;
        this.uid = uid;
        this.markersToRemove = markersToRemove;
        this.settings = settings;
        this.activity = activity;
        this.users = activity.users;
        this.markerIds = markerIds;
    }

    public void run() {
        System.out.println("REMOVE MARKER TASK WAITING FOR LOCK");
        final long lockStamp = settingsWriteLock.writeLock();
        System.out.println("REMOVE MARKER TASK ACQUIRED LOCK");

        String mapJSONString = settings.getString("map", "");
        HashMap<MarkerOptions, String[]> markerDetails = new HashMap<>();
        ArrayList<String> currencies = new ArrayList<>();
        JSONObject mapJSON = new JSONObject();

        // First we parse the map so we can remove the markers from the map JSON

        try {
            mapJSON = new JSONObject(mapJSONString);
            JSONArray markersJSON = mapJSON.getJSONArray("features");
            int removed = 0;
            int i = 0;
            while (i < markersJSON.length() && removed < markersToRemove.size()) {
                JSONObject markerJSON = markersJSON.getJSONObject(i);
                for (MarkerOptions markerOpt : markersToRemove) {
                    String markerId = markerIds.getOrDefault(markerOpt, "");
                    // If the marker we're removing matches the ID of the marker in the JSON, then
                    // the marker corresponds to the JSONObject in question
                    if (markerId.equals(markerJSON.getJSONObject("properties").getString("id"))) {
                        // We extract details on the marker for use later
                        String currency = markerJSON.getJSONObject("properties").getString("currency");
                        String value = markerJSON.getJSONObject("properties").getString("value");
                        markerDetails.put(markerOpt, new String[]{
                                currency,
                                value
                        });
                        // We keep a record of the currencies that will need to be updated so we don't
                        // update currencies that don't need updating
                        if (!currencies.contains(currency)) {
                            currencies.add(currency);
                        }
                        // Remove the JSON object that corresponds the the marker from the JSON of the
                        // map
                        markersJSON.remove(i);
                        // We decrement i as removing the current object means the position of the next
                        // object is at the current value of i, so decrementing ensures that when we
                        // increment at the end of this while loop that we're poining at the right
                        // position in the JSON array
                        i--;
                        removed++;

                        break;
                    }
                }
                i++;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // If we removed at least 1 marker from the map JSON then we update the database
        if (markerDetails.size() > 0) {
            final JSONObject mapJSONFinal = mapJSON;
            final DocumentReference docRef = db.collection(users).document(uid);
            final String newMapJSONString = mapJSONFinal.toString();
            // Use transactions as opposed to just querying the database and then writing it as transactions
            // ensure no writes have occured to the fields since they were read, preventing potential
            // synchronization errors
            if (activity.isNetworkAvailable()) {
                // If we're online update the database
                db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef);
                    Map<String, Object> newValues = new HashMap<>();
                    // Get the current values in the database of all the currencies we changed
                    for (String currency : currencies) {
                        newValues.put(currency, snapshot.getDouble(currency));
                    }
                    // Increment the amounts of each markers respective currency by the value specified
                    for (MarkerOptions marker : markerDetails.keySet()) {
                        String currency = markerDetails.get(marker)[0];
                        Double value = Double.parseDouble(markerDetails.get(marker)[1]);
                        newValues.put(currency, (double) newValues.get(currency) + value);
                    }
                    newValues.put("map", newMapJSONString);
                    transaction.update(docRef, newValues);

                    // Success
                    return newValues;
                }).addOnSuccessListener(newValues -> {
                    // Update the local values
                    SharedPreferences.Editor editor = settings.edit();
                    for (String currency : currencies) {
                        editor.putString(currency, Double.toString((double) newValues.get(currency)));
                    }
                    editor.putString("map", newMapJSONString);
                    editor.apply();
                    // Callback to MapFragment to complete removing markers
                    context.onMarkerRemoved(lockStamp, markerDetails, mapJSONFinal);
                }).addOnFailureListener(e -> {
                    System.out.println("Task failed with exception " + e);
                    settingsWriteLock.unlockWrite(lockStamp);
                });
            } else {
                // If we're offline keep track of the deltas
                HashMap<String, Double> newDeltas = new HashMap<>();
                // Get the current values of each of the change currencies deltas
                for (String currency : currencies) {
                    newDeltas.put(currency, Double.parseDouble(settings.getString(currency + "Delta", "0")));
                }
                // Increment the amounts of each markers respective currencies delta by the value specified
                for (MarkerOptions marker : markerDetails.keySet()) {
                    String currency = markerDetails.get(marker)[0];
                    Double value = Double.parseDouble(markerDetails.get(marker)[1]);
                    newDeltas.put(currency, newDeltas.get(currency) + value);
                }
                // Update the deltas in the settings
                SharedPreferences.Editor editor = settings.edit();
                for (String currency : currencies) {
                    editor.putString(currency + "Delta", Double.toString(newDeltas.get(currency)));
                }
                editor.putString("map", newMapJSONString);
                editor.apply();
                // If we're not already waitingToUpdateCoins, mark that we are
                if (!activity.waitingToUpdateCoins) {
                    activity.setToUpdateCoinsOnInternet();
                }
                // Callback to MapFragment to complete removing markers
                context.onMarkerRemoved(lockStamp, markerDetails, mapJSONFinal);
            }
        } else {
            settingsWriteLock.unlockWrite(lockStamp);
            System.out.println("REMOVE MARKER TASK RELEASED LOCK");
        }
    }
}

interface RemoveMarkersCallback {
    void onMarkerRemoved(long lockStamp, HashMap<MarkerOptions, String[]> markerDetails, JSONObject mapJSON);
}
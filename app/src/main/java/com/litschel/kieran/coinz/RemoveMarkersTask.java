package com.litschel.kieran.coinz;


import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

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

        try {
            mapJSON = new JSONObject(mapJSONString);
            JSONArray markersJSON = mapJSON.getJSONArray("features");
            int removed = 0;
            int i = 0;
            while (i < markersJSON.length() && removed < markersToRemove.size()) {
                JSONObject markerJSON = markersJSON.getJSONObject(i);
                for (MarkerOptions markerOpt : markersToRemove) {
                    String markerTitle = markerIds.getOrDefault(markerOpt, "");
                    Marker marker = markerOpt.getMarker();
                    if (markerTitle.equals(markerJSON.getJSONObject("properties").getString("id"))) {
                        String currency = markerJSON.getJSONObject("properties").getString("currency");
                        String value = markerJSON.getJSONObject("properties").getString("value");
                        markerDetails.put(markerOpt, new String[]{
                                currency,
                                value
                        });
                        if (!currencies.contains(currency)) {
                            currencies.add(currency);
                        }
                        markersJSON.remove(i);
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

        if (markerDetails.size() > 0) {
            final JSONObject mapJSONFinal = mapJSON;
            final DocumentReference docRef = db.collection(users).document(uid);
            // Use transactions as opposed to just querying the database and then writing it as transactions
            // ensure no writes have occured to the fields since they were read, preventing potential
            // synchronization errors
            if (activity.isNetworkAvailable()) {
                db.runTransaction(new Transaction.Function<Void>() {
                    @Override
                    public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                        DocumentSnapshot snapshot = transaction.get(docRef);
                        Map<String, Object> newValues = new HashMap<>();
                        for (String currency : currencies) {
                            newValues.put(currency, snapshot.getDouble(currency));
                        }
                        for (MarkerOptions marker : markerDetails.keySet()) {
                            String currency = markerDetails.get(marker)[0];
                            Double value = Double.parseDouble(markerDetails.get(marker)[1]);
                            newValues.put(currency, (double) newValues.get(currency) + value);
                        }
                        SharedPreferences.Editor editor = settings.edit();
                        for (String currency : currencies) {
                            editor.putString(currency, Double.toString((double) newValues.get(currency)));
                        }
                        editor.apply();
                        transaction.update(docRef, newValues);

                        // Success
                        return null;
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        context.onCoinsUpdated(lockStamp, markerDetails, mapJSONFinal);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Task failed with exception " + e);
                    }
                });
            } else {
                HashMap<String, Double> newDeltas = new HashMap<>();
                for (String currency : currencies) {
                    newDeltas.put(currency, Double.parseDouble(settings.getString(currency + "Delta", "0")));
                }
                for (MarkerOptions marker : markerDetails.keySet()) {
                    String currency = markerDetails.get(marker)[0];
                    Double value = Double.parseDouble(markerDetails.get(marker)[1]);
                    newDeltas.put(currency, newDeltas.get(currency) + value);
                }
                SharedPreferences.Editor editor = settings.edit();
                for (String currency : currencies) {
                    editor.putString(currency + "Delta", Double.toString(newDeltas.get(currency)));
                }
                editor.apply();
                if (!activity.waitingToUpdateCoins) {
                    activity.setToUpdateCoinsOnInternet();
                }
                context.onCoinsUpdated(lockStamp, markerDetails, mapJSONFinal);
            }
        } else {
            settingsWriteLock.unlockWrite(lockStamp);
            System.out.println("REMOVE MARKER TASK RELEASED LOCK");
        }
    }
}

interface RemoveMarkersCallback {
    void onCoinsUpdated(long lockStamp, HashMap<MarkerOptions, String[]> markerDetails, JSONObject mapJSON);
}
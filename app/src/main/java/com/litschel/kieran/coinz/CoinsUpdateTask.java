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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class CoinsUpdateTask implements Runnable {
    private CoinsUpdatedCallback context;
    private StampedLock mapUpdateLock;
    private FirebaseFirestore db;
    private String uid;
    private ArrayList<Marker> markersToRemove;
    private SharedPreferences settings;

    CoinsUpdateTask(CoinsUpdatedCallback context, StampedLock mapUpdateLock, FirebaseFirestore db, String uid, ArrayList<Marker> markersToRemove, SharedPreferences settings) {
        super();
        this.context = context;
        this.mapUpdateLock = mapUpdateLock;
        this.db = db;
        this.uid = uid;
        this.markersToRemove = markersToRemove;
        this.settings = settings;
    }

    public void run() {
        System.out.println("COINS UPDATE TASK WAITING FOR LOCK");
        final long lockStamp = mapUpdateLock.writeLock();
        System.out.println("COINS UPDATE TASK ACQUIRED LOCK");

        String mapJSONString = settings.getString("map", "");
        HashMap<Marker, String[]> markerDetails = new HashMap<>();
        ArrayList<String> currencies = new ArrayList<>();
        JSONObject mapJSON = new JSONObject();

        try {
            mapJSON = new JSONObject(mapJSONString);
            JSONArray markersJSON = mapJSON.getJSONArray("features");
            int removed = 0;
            int i = 0;
            while (i < markersJSON.length() && removed < markersToRemove.size()) {
                JSONObject markerJSON = markersJSON.getJSONObject(i);
                for (Marker marker : markersToRemove) {
                    if (marker.getTitle().equals(markerJSON.getJSONObject("properties").getString("id"))) {
                        String currency = markerJSON.getJSONObject("properties").getString("currency");
                        String value = markerJSON.getJSONObject("properties").getString("value");
                        markerDetails.put(marker, new String[]{
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
            final DocumentReference docRef = db.collection("users").document(uid);
            // Use transactions as opposed to just querying the database and then writing it as transactions
            // ensure no writes have occured to the fields since they were read, preventing potential
            // synchronization errors
            db.runTransaction(new Transaction.Function<Void>() {
                @Override
                public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                    DocumentSnapshot snapshot = transaction.get(docRef);
                    Map<String, Object> newValues = new HashMap<>();
                    for (String currency : currencies) {
                        newValues.put(currency, snapshot.getDouble(currency));
                    }
                    for (Marker marker : markerDetails.keySet()) {
                        String currency = markerDetails.get(marker)[0];
                        Double value = Double.parseDouble(markerDetails.get(marker)[1]);
                        newValues.put(currency, (double) newValues.get(currency) + value);
                    }
                    SharedPreferences.Editor editor = settings.edit();
                    for (String currency : currencies) {
                        editor.putString(currency,Double.toString((double) newValues.get(currency)));
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
            mapUpdateLock.unlockWrite(lockStamp);
            System.out.println("COINS UPDATE TASK RELEASED LOCK");
        }
    }
}

interface CoinsUpdatedCallback {
    void onCoinsUpdated(long lockStamp, HashMap<Marker, String[]> markerDetails, JSONObject mapJSON);
}
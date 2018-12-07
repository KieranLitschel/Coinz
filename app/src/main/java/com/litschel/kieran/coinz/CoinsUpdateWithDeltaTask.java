package com.litschel.kieran.coinz;

import android.content.SharedPreferences;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

// This task is used to update the databases values with the deltas we kept track of whilst offline

public class CoinsUpdateWithDeltaTask implements Runnable {
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private String users;
    private MainActivity activity;
    private CoinsUpdateWithDeltaCallback context;
    private FirebaseFirestore db;
    private SharedPreferences settings;
    private StampedLock settingsWriteLock;
    private String uid;

    CoinsUpdateWithDeltaTask(String users, CoinsUpdateWithDeltaCallback context, FirebaseFirestore db, SharedPreferences settings, StampedLock settingsWriteLock, String uid) {
        this.context = context;
        this.db = db;
        this.settings = settings;
        this.settingsWriteLock = settingsWriteLock;
        this.uid = uid;
        this.users = users;
        this.activity = ((MainActivity) context);
    }

    @Override
    public void run() {
        // First we acquire the lock to the settings to ensure no more changes are written before we've updated the database
        System.out.println("COIN UPDATE TASK WAITING FOR LOCK");
        long lockStamp = settingsWriteLock.writeLock();
        System.out.println("COIN UPDATE TASK ACQUIRED THE LOCK");
        // Update the users document via a database transaction, we use a transaction as opposed to reading and writing
        // to avoid potential issues caused by concurrent writes
        DocumentReference docRef = db.collection(users).document(uid);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            Map<String, Object> newValues = new HashMap<>();

            // Update the value of each currency in the database with the delta

            for (String currency : currencies) {
                Double currencyVal = snapshot.getDouble(currency);
                if (currencyVal != null) {
                    newValues.put(currency, currencyVal + Double.parseDouble(settings.getString(currency + "Delta", "0")));
                } else {
                    throw new FirebaseFirestoreException(String.format("USERS VALUE FOR %s DOES NOT EXIST IN THEIR FIRESTORE DOCUMENT", currency), FirebaseFirestoreException.Code.ABORTED);
                }
            }

            // If the date when we last downloaded the coins is today then the map will not have been updated.
            // This means we need to update the database to reflect the changes in coinsRemainingToday since offline
            // and the markers removed since offline

            if (LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString())).isEqual(activity.localDateNow())) {
                Double coinsRemainingToday = snapshot.getDouble("coinsRemainingToday");
                if (coinsRemainingToday != null) {
                    newValues.put("coinsRemainingToday", coinsRemainingToday + Double.parseDouble(settings.getString("coinsRemainingTodayDelta", "0")));
                    newValues.put("map", settings.getString("map", ""));
                } else {
                    throw new FirebaseFirestoreException("USERS VALUE FOR coinsRemainingToday DOES NOT EXIST IN THEIR FIRESTORE DOCUMENT", FirebaseFirestoreException.Code.ABORTED);
                }
            }

            transaction.update(docRef, newValues);

            // Success
            return null;
        }).addOnSuccessListener(aVoid -> {
            SharedPreferences.Editor editor = settings.edit();
            for (String currency : currencies) {
                // Update the local values with the deltas now that they have been pushed to the database
                editor.putString(currency, Double.toString(
                        Double.parseDouble(settings.getString(currency, "0"))
                                + Double.parseDouble(settings.getString(currency + "Delta", "0"))));
                // Delete the deltas so they are ready for when we next go offline
                editor.remove(currency + "Delta");
            }
            // If we had to update coinsRemainingToday in the database then we need to update it locally too
            if (LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString())).isEqual(activity.localDateNow())) {
                editor.putString("coinsRemainingToday", Double.toString(
                        Double.parseDouble(settings.getString("coinsRemainingToday", "0"))
                                + Double.parseDouble(settings.getString("coinsRemainingTodayDelta", "0"))));
                editor.remove("coinsRemainingTodayDelta");
                editor.remove("lostConnectionDate");
            }
            editor.apply();
            System.out.println("UPDATE COINS WITH DELTA TASK SUCCEEDED");
            context.onCoinsUpdateWithDeltaComplete(lockStamp);
        }).addOnFailureListener(e -> {
            System.out.printf("UPDATE COINS WITH DELTA TASK FAILED WITH EXCEPTION:\n%s", e.getMessage());
            context.onCoinsUpdateWithDeltaComplete(lockStamp);
        });
    }
}

interface CoinsUpdateWithDeltaCallback {
    void onCoinsUpdateWithDeltaComplete(long lockStamp);
}
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class CoinsUpdateWithDeltaTask implements Runnable {
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private CoinsUpdateWithDeltaCallback context;
    private FirebaseFirestore db;
    private SharedPreferences settings;
    private StampedLock mapUpdateLock;
    private String uid;
    private String users;

    CoinsUpdateWithDeltaTask(String users, CoinsUpdateWithDeltaCallback context, FirebaseFirestore db, SharedPreferences settings, StampedLock mapUpdateLock, String uid) {
        this.context = context;
        this.db = db;
        this.settings = settings;
        this.mapUpdateLock = mapUpdateLock;
        this.uid = uid;
        this.users = users;
    }

    @Override
    public void run() {
        long lockStamp = mapUpdateLock.writeLock();
        DocumentReference docRef = db.collection(users).document(uid);
        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(docRef);
                Map<String, Object> newValues = new HashMap<>();

                for (String currency : currencies) {
                    newValues.put(currency, snapshot.getDouble(currency) + Double.parseDouble(settings.getString(currency + "Delta", "0")));
                }

                if (LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString())).isEqual(LocalDate.now())) {
                    newValues.put("coinsRemainingToday", snapshot.getDouble("coinsRemainingToday") + Double.parseDouble(settings.getString("coinsRemainingTodayDelta", "0")));
                    newValues.put("map", settings.getString("map", ""));
                }

                transaction.update(docRef, newValues);

                // Success
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                SharedPreferences.Editor editor = settings.edit();
                for (String currency : currencies) {
                    editor.putString(currency, Double.toString(
                            Double.parseDouble(settings.getString(currency, "0"))
                                    + Double.parseDouble(settings.getString(currency + "Delta", "0"))));
                    editor.remove(currency + "Delta");
                }
                if (LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString())).isEqual(LocalDate.now())) {
                    editor.putString("coinsRemainingToday", Double.toString(
                            Double.parseDouble(settings.getString("coinsRemainingToday", "0"))
                                    + Double.parseDouble(settings.getString("coinsRemainingTodayDelta", "0"))));
                    editor.remove("coinsRemainingTodayDelta");
                    editor.remove("lostConnectionDate");
                }
                editor.apply();
                System.out.println("UPDATE COINS WITH DELTA TASK SUCCEEDED");
                context.onCoinsUpdateWithDeltaComplete(lockStamp);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("UPDATE COINS WITH DELTA TASK FAILED WITH EXCEPTION " + e);
                context.onCoinsUpdateWithDeltaComplete(lockStamp);
            }
        });
    }
}

interface CoinsUpdateWithDeltaCallback {
    void onCoinsUpdateWithDeltaComplete(long lockStamp);
}
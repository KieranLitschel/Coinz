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

public class ExecuteTradeTask implements Runnable {
    private ExecuteTradeTaskCallback context;
    private MainActivity activity;
    private FirebaseFirestore db;
    private StampedLock mapUpdateLock;
    private SharedPreferences settings;
    private HashMap<String, Double> currencyValues;
    private String currency;
    private Double coinsRemainingToday;
    private double tradeAmount;
    private double exchangeRate;

    ExecuteTradeTask(ExecuteTradeTaskCallback context, MainActivity activity, FirebaseFirestore db, SharedPreferences settings, HashMap<String, Double> currencyValues, Double coinsRemainingToday, String currency, double tradeAmount, double exchangeRate){
        this.context = context;
        this.activity = activity;
        this.db = db;
        this.mapUpdateLock = activity.mapUpdateLock;
        this.settings = settings;
        this.currencyValues = currencyValues;
        this.currency = currency;
        this.coinsRemainingToday = coinsRemainingToday;
        this.tradeAmount = tradeAmount;
        this.exchangeRate = exchangeRate;
    }

    @Override
    public void run() {
        long lockStamp = mapUpdateLock.writeLock();
        currencyValues.put(currency, currencyValues.get(currency) - tradeAmount);
        currencyValues.put("GOLD", currencyValues.get("GOLD") + tradeAmount * exchangeRate);
        coinsRemainingToday -= tradeAmount;
        if (activity.isNetworkAvailable()) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(currency, Double.toString(currencyValues.get(currency)));
            editor.putString("GOLD", Double.toString(currencyValues.get("GOLD")));
            editor.putString("coinsRemainingToday", Double.toString(coinsRemainingToday));
            editor.apply();
            DocumentReference docRef = db.collection("users").document(activity.uid);
            db.runTransaction(new Transaction.Function<Void>() {
                @Override
                public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                    DocumentSnapshot snapshot = transaction.get(docRef);
                    // Recalculate new values using value in database in case somehow the local stored
                    // value gets out of sync
                    Map<String, Object> updatedVals = new HashMap<>();
                    updatedVals.put(currency, snapshot.getDouble(currency) - tradeAmount);
                    updatedVals.put("GOLD", snapshot.getDouble("GOLD") + tradeAmount * exchangeRate);
                    updatedVals.put("coinsRemainingToday", snapshot.getDouble("coinsRemainingToday") - tradeAmount);
                    transaction.update(docRef, updatedVals);

                    // Success
                    return null;
                }
            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    System.out.println("Succeeded in updating database post trade");
                    mapUpdateLock.unlockWrite(lockStamp);
                    context.onTradeComplete(coinsRemainingToday);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    System.out.println("Failed to update database post trade with exception " + e);
                    mapUpdateLock.unlockWrite(lockStamp);
                    context.onTradeComplete(coinsRemainingToday);
                }
            });
        } else {
            // If there's no internet instead of updating the database we update the deltas, which are the changes in value
            // since the last connection to the database
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(currency + "Delta",
                    Double.toString(Double.parseDouble(settings.getString(currency + "Delta", "0")) - tradeAmount));
            editor.putString("GOLDDelta",
                    Double.toString(Double.parseDouble(settings.getString("GOLDDelta", "0")) + tradeAmount * exchangeRate));
            LocalDate lostConnectionDate = LocalDate.parse(settings.getString("lostConnectionDate", LocalDate.MIN.toString()));
            // We check that the date hasn't changed since we started to count how many coins the user has exchanged today
            if (lostConnectionDate.isEqual(LocalDate.now())) {
                editor.putString("coinsRemainingTodayDelta",
                        Double.toString(Double.parseDouble(settings.getString("coinsRemainingTodayDelta", "0")) - tradeAmount));
            } else {
                // If the date has changed we restart counting
                editor.putString("lostConnectionDate", LocalDate.now().toString());
                editor.putString("coinsRemainingTodayDelta",
                        Double.toString(tradeAmount));
            }
            editor.apply();
            if (!activity.waitingToUpdateCoins){
                activity.waitingToUpdateCoins = true;
                activity.setToUpdateCoinsOnInternet();
            }
            mapUpdateLock.unlockWrite(lockStamp);
            context.onTradeComplete(coinsRemainingToday);
        }
    }
}

interface ExecuteTradeTaskCallback{
    void onTradeComplete(double coinsRemainingToday);
}
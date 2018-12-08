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

// This activity executes a trade in Firestore and updates local values to reflect the trade

public class ExecuteTradeTask implements Runnable {
    private ExecuteTradeTaskCallback context;
    private MainActivity activity;
    private FirebaseFirestore db;
    private StampedLock settingsWriteLock;
    private SharedPreferences settings;
    private HashMap<String, Double> currencyValues;
    private String currency;
    private Double coinsRemainingToday;
    private double tradeAmount;
    private double exchangeRate;
    private String users;

    ExecuteTradeTask(String users, ExecuteTradeTaskCallback context, MainActivity activity, FirebaseFirestore db, SharedPreferences settings, HashMap<String, Double> currencyValues, Double coinsRemainingToday, String currency, double tradeAmount, double exchangeRate) {
        this.context = context;
        this.activity = activity;
        this.db = db;
        this.settingsWriteLock = activity.settingsWriteLock;
        this.settings = settings;
        this.currencyValues = currencyValues;
        this.currency = currency;
        this.coinsRemainingToday = coinsRemainingToday;
        this.tradeAmount = tradeAmount;
        this.exchangeRate = exchangeRate;
        this.users = users;
    }

    @Override
    public void run() {
        long lockStamp = settingsWriteLock.writeLock();
        if (activity.isNetworkAvailable()) {
            DocumentReference docRef = db.collection(users).document(activity.uid);
            db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef);

                // Update values in database to reflect trade
                Map<String, Object> updatedVals = new HashMap<>();
                Double dbAmountOfCurrency = snapshot.getDouble(currency);
                Double dbAmountOfGold = snapshot.getDouble("GOLD");
                Double dbCoinsRemainingToday = snapshot.getDouble("coinsRemainingToday");
                if (dbAmountOfCurrency != null & dbAmountOfGold != null & dbCoinsRemainingToday != null) {
                    updatedVals.put(currency, dbAmountOfCurrency - tradeAmount);
                    updatedVals.put("GOLD", dbAmountOfGold + tradeAmount * exchangeRate);
                    updatedVals.put("coinsRemainingToday", dbCoinsRemainingToday - tradeAmount);
                    transaction.update(docRef, updatedVals);
                } else {
                    throw new FirebaseFirestoreException(
                            String.format("EXPECT USERS DOCUMENT TO CONTAIN VALUES FOR %s, GOLD, AND" +
                                    " coinsRemainingToday, BUT AT LEAST ONE IS NOT DECLARED", currency),
                            FirebaseFirestoreException.Code.ABORTED);
                }

                // Success
                return null;
            }).addOnSuccessListener(aVoid -> {
                // Update local values to reflect database change
                currencyValues.put(currency, currencyValues.get(currency) - tradeAmount);
                currencyValues.put("GOLD", currencyValues.get("GOLD") + tradeAmount * exchangeRate);
                coinsRemainingToday -= tradeAmount;
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(currency, Double.toString(currencyValues.get(currency)));
                editor.putString("GOLD", Double.toString(currencyValues.get("GOLD")));
                editor.putString("coinsRemainingToday", Double.toString(coinsRemainingToday));
                editor.apply();
                System.out.println("Succeeded in updating database post trade");
                settingsWriteLock.unlockWrite(lockStamp);
                context.onTradeComplete(coinsRemainingToday);
            }).addOnFailureListener(e -> {
                System.out.printf("Failed to update database post trade with exception:\n%s\n", e.getMessage());
                settingsWriteLock.unlockWrite(lockStamp);
                context.onTradeComplete(coinsRemainingToday);
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
            if (lostConnectionDate.isEqual(activity.localDateNow())) {
                editor.putString("coinsRemainingTodayDelta",
                        Double.toString(Double.parseDouble(settings.getString("coinsRemainingTodayDelta", "0")) - tradeAmount));
            } else {
                // If the date has changed we restart counting
                editor.putString("lostConnectionDate", activity.localDateNow().toString());
                editor.putString("coinsRemainingTodayDelta",
                        Double.toString(tradeAmount));
            }
            editor.apply();
            if (!activity.waitingToUpdateCoins) {
                activity.setToUpdateCoinsOnInternet();
            }
            settingsWriteLock.unlockWrite(lockStamp);
            context.onTradeComplete(coinsRemainingToday);
        }
    }
}

interface ExecuteTradeTaskCallback {
    void onTradeComplete(double coinsRemainingToday);
}
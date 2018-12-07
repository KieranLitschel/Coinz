package com.litschel.kieran.coinz;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.concurrent.locks.StampedLock;

// This task updates the shared preferences files coin values, it is done on a seperate thread so
// we can use locks to ensure that there are no errors made as a result of concurrent writes.

public class CoinsUpdateTask implements Runnable {
    private CoinsUpdateTaskCallback context;
    private StampedLock settingsWriteLock;
    private SharedPreferences settings;
    private HashMap<String,Double> currencyChanges;

    CoinsUpdateTask(CoinsUpdateTaskCallback context, StampedLock settingsWriteLock, SharedPreferences settings, HashMap<String,Double> currencyChanges) {
        this.context = context;
        this.settingsWriteLock = settingsWriteLock;
        this.settings = settings;
        this.currencyChanges = currencyChanges;
    }

    @Override
    public void run() {
        // Wait for the write lock
        System.out.println("COIN UPDATE TASK WAITING FOR LOCK");
        long lockStamp = settingsWriteLock.writeLock();
        System.out.println("COIN UPDATE TASK ACQUIRED THE LOCK");
        // Write the settings file with the updated coin values
        SharedPreferences.Editor editor = settings.edit();
        for (String currency : currencyChanges.keySet()){
            double newValue = Double.parseDouble(settings.getString(currency,"0"))+currencyChanges.get(currency);
            editor.putString(currency, Double.toString(newValue));
        }
        editor.apply();
        settingsWriteLock.unlockWrite(lockStamp);
        context.coinsUpdateTaskComplete();
    }
}

interface CoinsUpdateTaskCallback {
    void coinsUpdateTaskComplete();
}

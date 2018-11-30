package com.litschel.kieran.coinz;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.concurrent.locks.StampedLock;

public class CoinsUpdateTask implements Runnable {
    private CoinsUpdateTaskCallback context;
    private StampedLock mapUpdateLock;
    private SharedPreferences settings;
    private HashMap<String,Double> currencyChanges;

    CoinsUpdateTask(CoinsUpdateTaskCallback context, StampedLock mapUpdateLock, SharedPreferences settings, HashMap<String,Double> currencyChanges) {
        this.context = context;
        this.mapUpdateLock = mapUpdateLock;
        this.settings = settings;
        this.currencyChanges = currencyChanges;
    }

    @Override
    public void run() {
        long lockStamp = mapUpdateLock.writeLock();
        SharedPreferences.Editor editor = settings.edit();
        for (String currency : currencyChanges.keySet()){
            double newValue = Double.parseDouble(settings.getString(currency,"0"))+currencyChanges.get(currency);
            editor.putString(currency, Double.toString(newValue));
        }
        editor.apply();
        mapUpdateLock.unlockWrite(lockStamp);
        context.coinsUpdateTaskComplete();
    }
}

interface CoinsUpdateTaskCallback {
    void coinsUpdateTaskComplete();
}

package com.litschel.kieran.coinz;

import android.content.SharedPreferences;

import java.util.concurrent.locks.StampedLock;

public class CoinsUpdateTask implements Runnable {
    private CoinsUpdateTaskCallback context;
    private StampedLock mapUpdateLock;
    private SharedPreferences settings;
    private String currency;
    private double changeInCurrency;

    CoinsUpdateTask(CoinsUpdateTaskCallback context, StampedLock mapUpdateLock, SharedPreferences settings, String currency, double changeInCurrency) {
        this.context = context;
        this.mapUpdateLock = mapUpdateLock;
        this.settings = settings;
        this.currency = currency;
        this.changeInCurrency = changeInCurrency;
    }

    @Override
    public void run() {
        long lockStamp = mapUpdateLock.writeLock();
        double newValue = Double.parseDouble(settings.getString(currency, "0")) + changeInCurrency;
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(currency, Double.toString(newValue));
        editor.apply();
        mapUpdateLock.unlockWrite(lockStamp);
        context.coinsUpdateTaskComplete();
    }
}

interface CoinsUpdateTaskCallback {
    void coinsUpdateTaskComplete();
}

package com.litschel.kieran.coinz;

import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.concurrent.locks.StampedLock;

public class MapUpdateTask implements Runnable {

    private StampedLock mapUpdateLock;
    private MapUpdateCallback context;
    private SharedPreferences settings;

    MapUpdateTask(MapUpdateCallback context, StampedLock mapUpdateLock, SharedPreferences settings) {
        super();
        this.mapUpdateLock = mapUpdateLock;
        this.context = context;
        this.settings = settings;
    }

    @Override
    public void run() {
        System.out.println("MAP UPDATE TASK WAITING FOR LOCK");
        long lockStamp = mapUpdateLock.writeLock();
        System.out.println("MAP UPDATE TASK ACQUIRED LOCK");

        LocalDate lastDownloadDate = LocalDate.parse(settings.getString("lastDownloadDate", LocalDate.MIN.toString()));

        if (lastDownloadDate.isBefore(LocalDate.now())) {
            context.updateMap(lockStamp);
        } else {
            mapUpdateLock.unlockWrite(lockStamp);
            System.out.println("MAP UPDATE TASK RELEASED LOCK");
            System.out.println("MAP IS UP TO DATE");
        }
    }
}

interface MapUpdateCallback {
    void updateMap(long lockStamp);
}
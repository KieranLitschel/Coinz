package com.litschel.kieran.coinz;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.locks.StampedLock;

// Downloads the map

public class DownloadMapTask extends AsyncTask<String, Void, String> {
    private MapDownloadedCallback context;
    private long lockStamp;
    private StampedLock settingsWriteLock;

    DownloadMapTask(MapDownloadedCallback context, StampedLock settingsWriteLock, long lockStamp) {
        super();
        this.context = context;
        this.lockStamp = lockStamp;
        this.settingsWriteLock = settingsWriteLock;
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            return loadFileFromNetwork(urls[0]);
        } catch (IOException e) {
            settingsWriteLock.unlockWrite(lockStamp);
            return "FAILED";
        }
    }

    private String loadFileFromNetwork(String urlString) throws IOException {
        return readStream(downloadUrl(new URL(urlString)));
    }

    private InputStream downloadUrl(URL url) throws IOException {
        System.out.println("Making GET request to JSON server");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000); // milliseconds
        conn.setConnectTimeout(15000); // milliseconds
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        System.out.println("Got response from JSON server");
        return conn.getInputStream();
    }

    @NonNull
    private String readStream(InputStream stream)
            throws IOException {
        // Read input from stream, build result as a string
        System.out.println("Started processing JSON response");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder result = new StringBuilder();
        String line;
        line = reader.readLine();
        result.append(line);
        while ((line = reader.readLine()) != null) {
            result.append("\n");
            result.append(line);
        }
        return result.toString();
    }

    @Override
    protected void onPostExecute(String result) {
        System.out.println("Finished processing JSON response");
        super.onPostExecute(result);
        DownloadCompleteRunner.downloadMapComplete(result, context, lockStamp);
    }
}

class DownloadCompleteRunner {

    static void downloadMapComplete(String mapJSONString, MapDownloadedCallback context, long lockStamp) {
        if (!(mapJSONString.equals("FAILED"))) {
            // Callsback to the MapFragment that started the async task
            context.onMapDownloaded(mapJSONString, lockStamp);
        } else {
            System.out.println("FAILED TO DOWNLOAD MAP");
        }
    }
}

interface MapDownloadedCallback {
    void onMapDownloaded(String mapJSONString, long lockStamp);
}
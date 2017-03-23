package com.aware.plugin.survey;

import android.content.Context;
import android.content.Intent;
import android.location.*;
import android.util.Log;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.ContentValues.TAG;

/**
 * Created by Nick on 23/03/2017.
 */

public class DataManager {

    public static class ProviderManager extends Thread {

        private LocationProvider provider;

        private ConcurrentLinkedQueue<Location> toAdd;

        ProviderManager(LocationProvider provider) {
            this.provider = provider;
        }

        @Override
        public void run() {
            while (true) {
                if (!toAdd.isEmpty()) {
//                    provider.insert(null, Location);    //UNF
                }
            }
        }

        void addLocation(Location location) {
            toAdd.add(location);
        }
    }

    private static ProviderManager provide = new ProviderManager(new LocationProvider());

    //default constructor should be replaced with more useful constructor in future
    public DataManager() {

    }

    public void giveLocation(Context context, Intent intent, Location location) {
        if (isNoteworthy(location)) {
            Log.d(TAG, "Passed Location");
            Log.d(TAG, location.toString());
            Plugin.onLocationReceive(context, intent,location);
        }
        else {
            Log.d(TAG, "Passed non-noteworthy location");
        }
    }

    public boolean isNoteworthy(Location location) {
        return location != null && location.getAccuracy() < 250;
    }
}

package com.aware.plugin.survey;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by Nick on 23/03/2017.
 */

public class DataManager {



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

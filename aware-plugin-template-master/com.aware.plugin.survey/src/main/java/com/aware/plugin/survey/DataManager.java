package com.aware.plugin.survey;

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

    public void giveLocation(Location location) {
        Log.d(TAG, "Passed Location");
        Log.d(TAG, location.toString());
    }

    public boolean isNoteworthy(Location location) {
        return location != null && location.getAccuracy() < 250;
    }

}

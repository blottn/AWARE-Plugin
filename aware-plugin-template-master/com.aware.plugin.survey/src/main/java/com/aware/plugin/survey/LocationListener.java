package com.aware.plugin.survey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.aware.providers.Accelerometer_Provider;
import com.aware.providers.Locations_Provider;

import static android.content.ContentValues.TAG;

/**
 * Created by Nick on 05/03/2017.
 */

public class LocationListener extends BroadcastReceiver {

    public LocationListener() {
        super();
    }

    public void onReceive(Context c, Intent intent) {
        Log.i(TAG, "Something was received");
        if (intent == null) {
            Log.i(TAG, "it was null");
        }
        else {
            Cursor data = c.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP + " DESC LIMIT 1");
            Log.i(TAG,data.toString());
            if (intent.getExtras() == null) {
                Log.i(TAG, "hmm the extras were null " + intent.getDataString());
//                Log.i(TAG, intent.toString());
            }
            Log.i(TAG, "Location pinged after null test");
        }
    }
}

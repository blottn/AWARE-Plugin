package com.aware.plugin.survey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import com.aware.providers.Accelerometer_Provider;
import com.aware.providers.Locations_Provider;

import static android.content.ContentValues.TAG;

/**
 * Created by Nicholas on 05/03/2017.
 */

public class LocationListener extends BroadcastReceiver {

    DataManager mgr;

    public LocationListener(DataManager mgr) {
        super();
        this.mgr = mgr;
    }

    public void onReceive(Context c, Intent intent) {
        Log.d(TAG, "Received a location broadcast");
        Cursor data = c.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP + " DESC LIMIT 1");
        if (data != null && data.moveToFirst()) {
            Location loc = new Location("Listener");
            loc.setLatitude(data.getDouble(data.getColumnIndex(Locations_Provider.Locations_Data.LATITUDE)));
            loc.setLongitude(data.getDouble(data.getColumnIndex(Locations_Provider.Locations_Data.LONGITUDE)));
            loc.setAccuracy(data.getInt(data.getColumnIndex(Locations_Provider.Locations_Data.ACCURACY)));
            loc.setTime(data.getLong(data.getColumnIndex(Locations_Provider.Locations_Data.TIMESTAMP)));
            mgr.giveLocation(loc);
        }
        data.close();
    }
}

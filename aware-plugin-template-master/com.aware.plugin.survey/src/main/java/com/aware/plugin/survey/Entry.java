package com.aware.plugin.survey;

import android.location.Location;
import android.util.Log;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.HashMap;

import static com.aware.Aware.TAG;

/**
 * Created by ronan on 03/04/2017.
 */

class Entry {
    Location location = null;
    HashMap<String, String> values;
    //these should all be constants
    final String esms = "esm_ids";
    final String name = "name";
    final String frequency = "frequency";
    final String activity = "activity";
    final String with = "with";
    final String lat = "lat";
    final String lon = "lon";
    final String accuracy = "accuracy";
    final String range = "range";
    final String time = "time";

    public Entry(Location loc){
        location = loc;
        values = new HashMap<>();
        values.put(lat,loc.getLatitude()+"");
        values.put(lon,loc.getLongitude()+"");
        values.put(accuracy, loc.getAccuracy() + "");
        values.put(name, "null");
        values.put(frequency, "null");
        values.put(activity, "null");
        values.put(with, "null");
        values.put(esms, "");
        values.put(range,"30"); //Default range of location
        values.put(time,longToString(new Time(loc.getTime()).getTime()));
    }

    public Entry(){
        location = null;
        values = new HashMap<>();
        values.put(lat,"null");
        values.put(lon,"null");
        values.put(accuracy, "null");
        values.put(name, "null");
        values.put(frequency, "null");
        values.put(activity, "null");
        values.put(with, "null");
        values.put(esms, "");
        values.put(range,"30"); //Default range of location
        values.put(time,"null");
    }

    public void put(String key,String val){
        if (key.equals(time)) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            Log.i(TAG, "-------------------------\n" +
                    "METHOD NAME: " + stackTraceElements[3]
                    + "Value : " + val);
        }
        values.put(key,val);
    }

    public String get(String key){
        return values.get(key);
    }

    String longToString(long l){
        String s="";
        while (l > 0) {
            s= (l%10)+"" + s;
            l=l/10;
        }
        Log.i("ENTRY","Changed long to " + s);
        return s;
    }
}

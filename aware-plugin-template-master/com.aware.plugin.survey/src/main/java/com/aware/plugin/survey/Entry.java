package com.aware.plugin.survey;

import android.location.Location;

import java.sql.Time;
import java.util.HashMap;
/**
 * Created by ronan on 03/04/2017.
 */

class Entry {
    Location location = null;
    HashMap<String, String> values;
    String name = "name";
    String frequency = "frequency";
    String activity = "activity";
    String with = "with";
    String lat = "lat";
    String lon = "lon";
    String accuracy = "accuracy";
    String range = "range";
    String time = "time";

    public Entry(Location loc){
        location = loc;
        values = new HashMap<>();
        values.put(lat,loc.getLatitude()+"");
        values.put(lon,loc.getLongitude()+"");
        values.put(accuracy, loc.getAccuracy() + "");
        values.put(name, "null");
        values.put(frequency, "null");
        values.put(activity, "null");
        values.put(range,"30"); //Default range of location
        values.put(time,new Time(loc.getTime()).getTime() + "");
    }

    public void put(String key,String val){
        values.put(key,val);
    }

    public String get(String key){
        return values.get(key);
    }
}

package com.aware.plugin.survey;

import android.location.Location;
import java.util.HashMap;
/**
 * Created by ronan on 03/04/2017.
 */

class Entry {
    HashMap<String, String> values;
    String name = "name";
    String frequency = "frequency";
    String activity = "activity";
    String with = "with";
    String lat = "lat";
    String lon = "lon";
    String accuracy = "accuracy";
    String range = "range";

    public Entry(Location loc){
        values = new HashMap<>();
        values.put(lat,loc.getLatitude()+"");
        values.put(lon,loc.getLongitude()+"");
        values.put(accuracy, loc.getAccuracy() + "");
        values.put(range,"30"); //Default range of location
    }

    public void put(String key,String val){
        values.put(key,val);
    }
}

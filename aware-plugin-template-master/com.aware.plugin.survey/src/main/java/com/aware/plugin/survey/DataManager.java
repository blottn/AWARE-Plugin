package com.aware.plugin.survey;

import android.content.Context;
import android.content.Intent;
import android.location.*;
import android.util.Log;

import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Freetext;

import org.json.JSONException;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.ContentValues.TAG;

/**
 * Created by Nick on 23/03/2017.
 */

public class DataManager {
    private Location previousLocation=null;
    private int negligibleRange=100;

    public static class ProviderManager extends Thread {

        private Provider provider;

        private ConcurrentLinkedQueue<Location> toAdd;

        ProviderManager(Provider provider) {
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

    private static ProviderManager provide = new ProviderManager(new Provider());

    //default constructor should be replaced with more useful constructor in future
    public DataManager() {

    }

    public void giveLocation(Context context, Intent intent, Location location) {
        if (isNoteworthy(location)) {
            Log.d(TAG, "Passed Location");
            Log.d(TAG, location.toString());
            onLocationReceive(context, intent,location);
        }
        else {
            Log.d(TAG, "Passed non-noteworthy location");
        }
    }

    public boolean isNoteworthy(Location location) {
        Log.i(TAG,"Checking if location is noteworthy");
        if(location == null || location.getAccuracy() > 250)
            return false;
        if(previousLocation==null){
            Log.i(TAG,"No previous location");
            previousLocation = location;
            return true;
        }
        if(distance(location.getLatitude(),location.getLongitude(),previousLocation.getLatitude(),
                    previousLocation.getLongitude())<negligibleRange){ //If points are within negligible range
            Log.i(TAG,"Location was negligible");
            return false;
        }
        return true;
    }

    public int distance(double lat1,double lon1,double lat2,double lon2) {
        double p = 0.017453292519943295;    // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p)/2 +
                Math.cos(lat1 * p) * Math.cos(lat2 * p) *
                        (1 - Math.cos((lon2 - lon1) * p))/2;

        return (int)(1000* 12742 * Math.asin(Math.sqrt(a))); // 2 * R; R = 6371 km
    }

    public void onLocationReceive(Context context, Intent intent,Location location){
        try {
            ESM_Freetext question = new ESM_Freetext();
            question.setTitle("Location Survey Questionnaire")
                    .setSubmitButton("OK")
                    .setInstructions("What is this location? " + location.getLatitude() + ", " + location.getLongitude());
            ESMFactory factory = new ESMFactory();
            factory.addESM(question);
            ESM.queueESM(context,factory.build());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param loc
     * @return Type of location: 0 = New(Not within previous location radius)
     *                           1 = Previous Visits < 10(Rare Location)
     *                           2 = Previous Visits > 10(Frequent Location)
     */
    public int getLocationType(Location loc){
        return 0;
    }
}

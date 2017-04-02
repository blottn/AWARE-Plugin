package com.aware.plugin.survey;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.*;
import android.util.Log;

import com.aware.ESM;
import com.aware.ui.esms.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.ContentValues.TAG;

/**
 * Created by Nick on 23/03/2017.
 */

public class DataManager {
    private static Location previousLocation=null;
    static Time startQuestions = null;
    static Time stopQuestions = null;
    private ConcurrentLinkedQueue<Location> toBeAnswered = new ConcurrentLinkedQueue<>();

    private final int NEGLIGIBLE_RANGE = 50;
    private final int TOLERABLE_ACCURACY = 250;

    protected static class ProviderManager extends Thread {

        private Provider provider;

        private ConcurrentLinkedQueue<MarkedLocation> toAdd;

        ProviderManager(Provider provider) {
            this.provider = provider;
        }

        @Override
        public void run() {
            while (true) {
                if (!toAdd.isEmpty()) {
                    MarkedLocation data = toAdd.poll();
                    ContentValues values = new ContentValues();
                    values.put("Name",data.name);
                    values.put("Latitude",data.location.getLatitude());
                    values.put("Timestamp",data.location.getTime());
                    values.put("Longitude",data.location.getLongitude());
                    values.put("Accuracy",data.location.getAccuracy());
                    provider.insert(Provider.TableOne_Data.CONTENT_URI, values);
                }
            }
        }

        void addMarkedLocation(MarkedLocation location) {
            toAdd.add(location);
        }

        Cursor mostRecent() {
            Cursor cursor = provider.query(Provider.TableOne_Data.CONTENT_URI, null, null, null, Provider.TableOne_Data.TIMESTAMP + " DESC 1");
            cursor.moveToFirst();
            return cursor;
        }
    }

    public static class MarkedLocation {
        public Location location;
        public String name;

        public MarkedLocation(String name, Location location) {
            this.name = name;
            this.location = location;
        }
    }

    private static ProviderManager provide = new ProviderManager(new Provider());

    //default constructor should be replaced with more useful constructor in future
    DataManager() {

    }

    void giveLocation(final Context context,final Location location) {

        if (isNoteworthy(location)) {
            onLocationReceive(context, location);
        }
        else {
            printLocation(location);
        }
    }

    void onESMAnswered(JSONObject info,String answer){
        if(info==null || answer==null)
            return;
        Location location = toBeAnswered.poll();
        storeData(info, answer, location);
    }

    private void storeData(JSONObject esmJson,String esmAnswer,Location location){
        try {
            Log.i(TAG, "\n-----------------------------------------\nAdding location to database:\nQuestion: "
                    + esmJson.getString("esm_instructions")
                    + "\nAnswer: "+ esmAnswer);
            printLocation(location);
            Log.i(TAG, "\n-----------------------------------------\n");
            MarkedLocation toStore = new MarkedLocation(esmAnswer, previousLocation);
            provide.addMarkedLocation(toStore);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isNoteworthy(Location location) {
        Log.i(TAG,"Checking if location is noteworthy");
        if(previousLocation==null){
            Log.i(TAG,"Not known previous location");
            previousLocation = location;
            return true;
        }
        if(location == null || location.getAccuracy() > TOLERABLE_ACCURACY || distance(location.getLatitude(),location.getLongitude(),previousLocation.getLatitude(),
                    previousLocation.getLongitude())< NEGLIGIBLE_RANGE){ //If points are within negligible range
            Log.i(TAG,"Location was negligible");
            return false;
        }
        previousLocation = location;
        return true;
    }

    private int distance(double lat1,double lon1,double lat2,double lon2) {
        double p = 0.017453292519943295;    // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p)/2 +
                Math.cos(lat1 * p) * Math.cos(lat2 * p) *
                        (1 - Math.cos((lon2 - lon1) * p))/2;

        return (int)(1000* 12742 * Math.asin(Math.sqrt(a))); // 2 * R; R = 6371 km
    }

    private void onLocationReceive(Context context,Location location){
        try {
            int locType = getLocationType(location);
            switch (locType){
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                default:
                    break;
            }
            ESM_Freetext question = new ESM_Freetext();
            Time t = new Time(location.getTime());
            question.setTitle("New Location")
                    .setSubmitButton("OK")
                    .setInstructions("Where were you at "+ t.toString()+ "? ");
            ESMFactory factory = new ESMFactory();
            factory.addESM(question);
            ESM.queueESM(context,factory.build());
            toBeAnswered.add(location);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param loc received location
     * @return Type of location: 0 = New(Not within previous location radius)
     *                           1 = Possibly previous location(Within specified distance)
     *                           2 = Definitely previous location(Within negligible distance)
     */
    private int getLocationType(Location loc){
        return 0;
    }

    private void printLocation(Location loc){
        if(loc==null){
            Log.i(TAG, "Location was null.");
            return;
        }
        Log.i(TAG,"\nPrinting Location:\n" + loc.getLatitude() + ", " + loc.getLongitude() +
                "\nTime:"+ new Time(loc.getTime()).toString() + "\nAccuracy: "+ loc.getAccuracy());
    }

    void onESMCancelled(){
        toBeAnswered.poll(); //Remove corresponding location
    }
}

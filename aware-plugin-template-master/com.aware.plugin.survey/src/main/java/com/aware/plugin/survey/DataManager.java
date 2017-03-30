package com.aware.plugin.survey;

import android.content.Context;
import android.content.Intent;
import android.location.*;
import android.nfc.Tag;
import android.util.Log;

import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Freetext;
import com.aware.ui.esms.ESM_Question;

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
    private Location loc = null;
    private JSONObject esmJson = null;
    private String esmAnswer = null;
    private boolean processing = false;

    private final int NEGLIGIBLE_RANGE = 50;
    private final int TOLERABLE_ACCURACY = 250;

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
    DataManager() {

    }

    void giveLocation(final Context context,final Location location) {
        if(isProcessing())
            return;
        processing = true;
        if (isNoteworthy(location)) {
            onLocationReceive(context, location);
        }
        else {
            processing = false;
            printLocation(location);
        }
    }

    void onESMAnswered(JSONObject info,String answer){
        if(info==null || answer==null)
            return;
        esmJson = info;
        esmAnswer = answer;
        storeData();
    }

    private void storeData(){
        /* TO DO: Store relevant data in database */
        try {
            Log.i(TAG, "\n-----------------------------------------\nNo database so here you go: \n" + esmJson.getString("esm_instructions")
                    + "\nAnswer: "+ esmAnswer);
            printLocation(loc);
            Log.i(TAG, "\n-----------------------------------------\n");


        } catch (JSONException e) {
            e.printStackTrace();
        }
        processing = false;
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
            loc = location;
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

    boolean isProcessing(){
        return processing;
    }

    private void printLocation(Location loc){
        if(loc==null){
            Log.i(TAG, "Location was null.");
            return;
        }
        Log.i(TAG,"\nPrinting Location:\n" + loc.getLatitude() + ", " + loc.getLongitude() +
                "\nTime:"+ new Time(loc.getTime()).toString() + "\nAccuracy: "+ loc.getAccuracy());
    }
}

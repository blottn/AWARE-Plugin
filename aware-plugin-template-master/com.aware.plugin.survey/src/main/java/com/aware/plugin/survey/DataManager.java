package com.aware.plugin.survey;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.*;
import android.util.Log;

import com.aware.ESM;
import com.aware.providers.Locations_Provider;
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
            toAdd = new ConcurrentLinkedQueue<MarkedLocation>();
        }

        @Override
        public void run() {
            while (true) {
                if (!toAdd.isEmpty()) {
                    MarkedLocation data = toAdd.poll();
                    Log.i(TAG, "inserting: " + data.location.toString());
                    ContentValues values = new ContentValues();
                    values.put(Provider.TableOne_Data.LOCATION_NAME,data.name);
                    values.put(Provider.TableOne_Data.LATITUDE,data.location.getLatitude());
                    values.put(Provider.TableOne_Data.TIMESTAMP,data.location.getTime());
                    values.put(Provider.TableOne_Data.LONGITUDE,data.location.getLongitude());
                    values.put(Provider.TableOne_Data.ACCURACY, (int)data.location.getAccuracy());
//                    provider.insert(Provider.TableOne_Data.CONTENT_URI, values);
                    Log.i(TAG, "Stored a location in the database");
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

        Cursor getLocationsWithin(int metres, int accuracy, Location location) {

            Location up, down, left, right;

            up = new Location(location);
            down = new Location(location);
            left = new Location(location);
            right = new Location(location);

            up.setLatitude(up.getLatitude() + (((double)metres) / 111111.00) );
            down.setLatitude(up.getLatitude() - (((double)metres) / 111111.00) );

            left.setLongitude(left.getLongitude() - (((double) metres) / 111111.00) * Math.cos(left.getLatitude() * 2 * Math.PI));
            right.setLongitude(right.getLongitude() + (((double) metres) / 111111.00) * Math.cos(right.getLatitude() * 2 * Math.PI));
            Cursor cursor = Plugin.context.getContentResolver().query(Provider.TableOne_Data.CONTENT_URI,
                        new String[] {Provider.TableOne_Data.LATITUDE, Provider.TableOne_Data.LONGITUDE, Provider.TableOne_Data.ACCURACY, Provider.TableOne_Data.LOCATION_NAME },
                        " WHERE ",
                        new String[] {
                                Provider.TableOne_Data.LATITUDE  + " BETWEEN " + down.getLatitude() + " " + up.getLatitude(),
                                Provider.TableOne_Data.LONGITUDE + " BETWEEN " + left.getLatitude() + " " + right.getLatitude(),
                                Provider.TableOne_Data.ACCURACY + "<" + accuracy
                        },
                        Provider.TableOne_Data.TIMESTAMP + " DESC LIMIT 1");
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

    private ProviderManager provide = new ProviderManager(new Provider());

    //default constructor should be replaced with more useful constructor in future
    DataManager() {
        provide.start();
    }

    void giveLocation(final Context context,final Location location) {
        //TODO remove hardcoded insert
//        provide.addMarkedLocation(new MarkedLocation("hello world", location));
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

    private void onLocationReceive(Context context, Location location){

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
    private int getLocationType(Location loc) {
        Cursor c = provide.getLocationsWithin(50, 50, loc);
        if (c == null || c.getCount() == 0) {
            c = provide.getLocationsWithin(100, 100, loc);
            if (c == null || c.getCount() == 0) {
                return 0;
            }
            else {
                return 1;
            }
        }
        else {
            return 2;
        }
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

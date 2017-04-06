package com.aware.plugin.survey;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.*;
import android.util.Log;

import com.aware.ESM;
import com.aware.ui.esms.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.ContentValues.TAG;

/**
 * Created by Nick on 23/03/2017.
 */

public class DataManager {
    static final String NEW_QUESTION_1 = "Where are you?";
    static final String NEW_QUESTION_2 = "How often do you come here?";
    static final String PREV_QUESTION_1 = "What type of activity are you doing?";
    static final String PREV_QUESTION_2 = "Who are you doing this activity with?";
    private static Location previousLocation = null;
    static Time startQuestions = null;
    static Time stopQuestions = null;
    private ConcurrentLinkedQueue<Entry> toBeAnswered = new ConcurrentLinkedQueue<>();
    int questionsPerQueue;
    Entry waiting; //Waiting for question queue to finish

    private final int NEGLIGIBLE_RANGE = 5;
    private final int TOLERABLE_ACCURACY = 250;

    protected static class ProviderManager extends Thread {

        private Provider provider;

        private ConcurrentLinkedQueue<Entry> toAdd;

        ProviderManager(Provider provider) {

            this.provider = provider;
            toAdd = new ConcurrentLinkedQueue<>();
        }

        @Override
        public void run() {
            while (true) {
                if (!toAdd.isEmpty()) {
                    Entry data = toAdd.poll();
                    Log.i(TAG, "inserting: " + data.values.get(data.name));
                    ContentValues values = new ContentValues();

//                    values.put(Provider.Location_Survey_Table.LOCATION_NAME, data.values.get("name"));
                    values.put(Provider.Location_Survey_Table.LATITUDE, Double.parseDouble(data.values.get("lat")));
                    values.put(Provider.Location_Survey_Table.TIMESTAMP, Long.parseLong(data.values.get("time")));    //needs a different time format
                    values.put(Provider.Location_Survey_Table.LONGITUDE, Double.parseDouble(data.values.get("lon")));
//                    values.put(Provider.Location_Survey_Table.ACCURACY, Integer.parseInt(data.values.get("accuracy")));
                    provider.insert(Provider.Location_Survey_Table.CONTENT_URI, values);
                    Log.i(TAG, "Stored a location in the database.");
                }
            }
        }

        void addEntry(Entry entry) {
            /*TODO: Check if there is an entry with the same name
            Vague idea of code:
                if(existingEntry(entry)){
                    Entry old = removeExisting();
                    updateInfo(entry,old); //Change frequency(if different) and range of old
                    toAdd.add(old);
                }
                else
                    toAdd.add(entry);
            */
            toAdd.add(entry);
        }

        Cursor getAll() {
            return Plugin.context.getContentResolver().query(Provider.Location_Survey_Table.CONTENT_URI,
                    null,
                    null,
                    null,
                    Provider.Location_Survey_Table.TIMESTAMP + " DESC");
        }

        Cursor mostRecent() {
            Cursor cursor = provider.query(Provider.Location_Survey_Table.CONTENT_URI, null, null, null, Provider.Location_Survey_Table.TIMESTAMP + " DESC 1");
            cursor.moveToFirst();
            return cursor;
        }

        Cursor getLocationsWithin(int metres, int accuracy, Location location) {
            Location up, down, left, right;
            up = new Location(location);
            down = new Location(location);
            left = new Location(location);
            right = new Location(location);

            up.setLatitude(up.getLatitude() + (((double) metres) / 111111.00));
            down.setLatitude(up.getLatitude() - (((double) metres) / 111111.00));

            left.setLongitude(left.getLongitude() - (((double) metres) / 111111.00) * Math.cos(left.getLatitude() * 2 * Math.PI));
            right.setLongitude(right.getLongitude() + (((double) metres) / 111111.00) * Math.cos(right.getLatitude() * 2 * Math.PI));
            Cursor cursor = Plugin.context.getContentResolver().query(Provider.Location_Survey_Table.CONTENT_URI,
                        new String[] {Provider.Location_Survey_Table.LOCATION_NAME, Provider.Location_Survey_Table.LONGITUDE, Provider.Location_Survey_Table.LATITUDE},// Provider.Location_Survey_Table.ACCURACY},
                            "(" + Provider.Location_Survey_Table.LATITUDE  + " BETWEEN " + down.getLatitude() + " AND " + up.getLatitude() + ") AND " +
                            "(" + Provider.Location_Survey_Table.LONGITUDE + " BETWEEN " + left.getLatitude() + " AND " + right.getLatitude() + ")",
//                            Provider.Location_Survey_Table.ACCURACY + "<" + accuracy,
                        null,
                        Provider.Location_Survey_Table.TIMESTAMP + " DESC LIMIT 1");
            return cursor;
        }

        void printAllRows() {
            Cursor cursor = Plugin.context.getContentResolver().query(Provider.Location_Survey_Table.CONTENT_URI,
                    null,
                    null,
                    null,
                    Provider.Location_Survey_Table.TIMESTAMP + " DESC LIMIT 1"
                    );

            Log.i(TAG, "here are all the rows of the database " + cursor.getCount());
            while (cursor != null && cursor.moveToNext()) {
                for (String column : cursor.getColumnNames()) {
                    Log.i(TAG, "Column: " + column + " value " + cursor.getString(cursor.getColumnIndex(column)));
                }
            }
        }

        void delete(String name) {
            Plugin.context.getContentResolver().delete(
                        Provider.Location_Survey_Table.CONTENT_URI,
                        Provider.Location_Survey_Table.LOCATION_NAME + "=" + name,
                        null);
        }

        boolean checkExists(String locationName) {
            Cursor cursor = Plugin.context.getContentResolver().query(
                    Provider.Location_Survey_Table.CONTENT_URI,
                    null,
                    Provider.Location_Survey_Table.LOCATION_NAME + "=" + locationName,
                    null,
                    Provider.Location_Survey_Table._ID + " DESC"
            );

            return !(cursor == null || cursor.getCount() == 0);

        }

    }
    private ProviderManager provider = new ProviderManager(new Provider());

    //default constructor should be replaced with more useful constructor in future
    DataManager() {
        provider.start();
    }

    void giveLocation(final Context context, final Location location) {
        provider.printAllRows();
        if (isNoteworthy(location)) {
            onLocationReceive(context, location);
        }
    }

    private boolean isNoteworthy(Location location) {
        Log.i(TAG, "Checking if location is noteworthy");
        if (location == null || location.getSpeed() > 4)
            return false;
        if (previousLocation == null) {
            Log.i(TAG, "Not known previous location");
            previousLocation = location;
            return true;
        }
        if (location == null || location.getAccuracy() > TOLERABLE_ACCURACY) {/* || distance(location.getLatitude(), location.getLongitude(), previousLocation.getLatitude(),
                previousLocation.getLongitude()) < NEGLIGIBLE_RANGE) { //If points are within negligible range*/
            Log.i(TAG, "Location was negligible");
            return false;
        }
        previousLocation = location;
        return true;
    }

    private void onLocationReceive(Context context, Location location) {
        try {
            Entry entry = isPreviousLocation(location);
            if (entry == null) {
                askNewLocation(context, location);
            }
            else{
                askPreviousLocation(context, entry);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Entry isPreviousLocation(Location loc) {
        Entry[] entries = getEntries();
        Entry closest=null;
        int closestDist=Integer.MAX_VALUE;
        int dist;
        for(Entry e:entries){
            dist=distance(Double.parseDouble(e.get(e.lat)),Double.parseDouble(e.get(e.lon))
                    ,loc.getLatitude(),loc.getLongitude());
            if (dist < Integer.parseInt(e.get(e.range)) && dist<closestDist) {
                closest=e;
            }
        }
        if (closest != null) {
            provider.delete(closest.get(closest.name));
        }
        return closest;
    }

    private Entry[] getEntries(){
        ArrayList<Entry> entriesList = new <Entry>ArrayList();
        Cursor c = provider.getAll();
        if(!c.moveToFirst()) //Check if database is empty
            return new Entry[0];
        boolean end = false;
        while (!end) {
            Entry e = new Entry();
            //Change database to entry array
            e.put(e.name,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LOCATION_NAME)));
            e.put(e.lat,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LATITUDE)));
            e.put(e.lon,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LONGITUDE)));
            e.put(e.accuracy,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.ACCURACY)));
            e.put(e.time,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.TIMESTAMP)));
            e.put(e.frequency,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.FREQUENCY)));
            e.put(e.activity,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.ACTIVITY)));
            e.put(e.with,c.getString(c.getColumnIndex(Provider.Location_Survey_Table.WITH)));
            entriesList.add(e);
            end=c.moveToNext();
        }
        Entry[] entries = new Entry[entriesList.size()];
        for(int i=0;i<entries.length;i++)
            entries[i]=entriesList.get(i);
        return entries;
    }

    private void askNewLocation(Context context, Location location) throws JSONException {
        ESM_Radio q1 = new ESM_Radio();
        ESM_Radio q2 = new ESM_Radio();
        ESMFactory factory1 = new ESMFactory();
        Entry[] entries = getEntries();
        for(Entry e:entries) q2.addRadio(e.get(e.name));
        q1.addRadio("Other")
                .setInstructions(NEW_QUESTION_1)
                .setTitle("New Location")
                .setSubmitButton("OK");
        factory1.addESM(q1);
        q2.addRadio("Daily")
                .addRadio("Weekly")
                .addRadio("Monthly")
                .addRadio("Less Often")
                .setInstructions(NEW_QUESTION_2)
                .setTitle("New Location")
                .setSubmitButton("OK");
        factory1.addESM(q2);
        questionsPerQueue = 2;
        ESM.queueESM(context, factory1.build());
        for (int i = 0; i < questionsPerQueue; i++)
            toBeAnswered.add(new Entry(location));
    }

    private void askPreviousLocation(Context context, Entry entry) throws JSONException {
        ESM_Radio q1 = new ESM_Radio();
        ESM_Radio q2 = new ESM_Radio();
        ESMFactory factory = new ESMFactory();
        Entry[] entries = getEntries();
        for(Entry e:entries) q2.addRadio(e.get(e.activity)); //Gets existing entries in database and displays as options
        q1.addRadio("Other") //Option to allow user to input new entry
                .setInstructions(PREV_QUESTION_1)
                .setTitle("Previous Location")
                .setSubmitButton("OK");
        factory.addESM(q1);
        for(Entry e:entries) q2.addRadio(e.get(e.with));
        q2.addRadio("Other")
                .setInstructions(PREV_QUESTION_2)
                .setTitle("Previous Location")
                .setSubmitButton("OK");
        factory.addESM(q2);
        questionsPerQueue = 2;
        ESM.queueESM(context, factory.build());
        for (int i = 0; i < questionsPerQueue; i++)
            toBeAnswered.add(entry);
    }

    /**
     * Checks if there are any issues with the answered ESM and passes on to be stored
     * @param info All the ESM details ie: Instructions, title etc.
     * @param answer The user's response to the ESM
     */
    void onESMAnswered(JSONObject info, String answer) {
        if (info == null || answer == null || toBeAnswered.isEmpty()) {
            Log.e(TAG, "Part of the ESM was null or the queue of locations was empty.");
            return;
        }
        Entry entry = toBeAnswered.poll();
        storeData(info, answer, entry);
    }

    /**
     * Checks what question was answered. If more questions to be added make sure to keep updating info
     * in waiting entry until last question answered then store entry.
     * @param esmJson   All the ESM details ie: Instructions, title etc.
     * @param esmAnswer The user's response to the ESM
     * @param entry The entry containing the location the ESM is based on
     */
    private void storeData(JSONObject esmJson, String esmAnswer, Entry entry) {

        try {
            String instructions = esmJson.getString("esm_instructions");
            switch (instructions){
                case NEW_QUESTION_1:
                    //Setting name value
                    entry.put(entry.name,esmAnswer);
                    waiting = entry;
                    break;
                case NEW_QUESTION_2:
                    //Setting frequency value
                    waiting.put(entry.frequency,esmAnswer);
                    provider.addEntry(waiting);
                    waiting = null;

                 case PREV_QUESTION_1:
                    //Setting activity value
                    entry.put(entry.activity,esmAnswer);
                    waiting = entry;

                case PREV_QUESTION_2:
                    //Setting with value
                    waiting.put(entry.with,esmAnswer);
                    provider.addEntry(waiting);
                    waiting = null;

                default:
                    Log.e(TAG,"Received answer to unknown ESM.\nClearing queue to be answered.");
                    toBeAnswered.clear();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     *Functions below not directly in timeline of events
     */

    void onESMCancelled() {
        Log.i(TAG, "ESM cancelled");
        toBeAnswered.clear(); //Remove corresponding location
    }

//    private static void printEntry(Entry loc) {
//        if (loc == null) {
//            Log.i(TAG, "Location was null.");
//            return;
//        }
//        Log.i(TAG,"Entry:\n"+
//                "Time: "+ loc.get(loc.time)+"\n" +
//                "Name: "+ loc.get(loc.name)+"\n" +
//                "Lat: "+ loc.get(loc.lat)+"\n" +
//                "Lon: "+ loc.get(loc.lon)+"\n" +
//                "Accuracy: "+ loc.get(loc.accuracy)+"\n" +
//                "Range: "+ loc.get(loc.range)+"\n" +
//                "Frequency: "+ loc.get(loc.frequency)+"\n" +
//                "Activity: "+ loc.get(loc.activity)+"\n" +
//                "With: "+ loc.get(loc.with)+"\n" );
//    }

    /**
     *
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return  Distance between the two points based on the Haversine method
     */
    private int distance(double lat1, double lon1, double lat2, double lon2) {
        double p = 0.017453292519943295;    // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2 +
                Math.cos(lat1 * p) * Math.cos(lat2 * p) *
                        (1 - Math.cos((lon2 - lon1) * p)) / 2;

        return (int) (1000 * 12742 * Math.asin(Math.sqrt(a))); // 2 * R; R = 6371 km
    }
}

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
    static final String NEW_QUESTION_1 = "Where are you?";
    static final String NEW_QUESTION_2 = "How often do you come here?";
    static final String PREV_QUESTION_1 = "What type of activity are you doing?";
    static final String PREV_QUESTION_2 = "Who are you doing this activity with?";
    private static Location previousLocation = null;
    static Time startQuestions = null;
    static Time stopQuestions = null;
    private ConcurrentLinkedQueue<Entry> toBeAnswered = new ConcurrentLinkedQueue<>();
    int questionsPerQueue;

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
                    Log.i(TAG, "inserting: " + data.values.get("name"));
                    ContentValues values = new ContentValues();
                    values.put(Provider.TableOne_Data.LOCATION_NAME, data.values.get("name"));
                    values.put(Provider.TableOne_Data.LATITUDE, Integer.parseInt(data.values.get("lat")));
                    values.put(Provider.TableOne_Data.TIMESTAMP, Integer.parseInt(data.values.get("time")));
                    values.put(Provider.TableOne_Data.LONGITUDE, Integer.parseInt(data.values.get("long")));
                    values.put(Provider.TableOne_Data.ACCURACY, Integer.parseInt(data.values.get("accuracy")));
//                    provider.insert(Provider.TableOne_Data.CONTENT_URI, values);
                    Log.i(TAG, "Stored a location in the database");
                }
            }
        }

        void addEntry(Entry location) {
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

            up.setLatitude(up.getLatitude() + (((double) metres) / 111111.00));
            down.setLatitude(up.getLatitude() - (((double) metres) / 111111.00));

            left.setLongitude(left.getLongitude() - (((double) metres) / 111111.00) * Math.cos(left.getLatitude() * 2 * Math.PI));
            right.setLongitude(right.getLongitude() + (((double) metres) / 111111.00) * Math.cos(right.getLatitude() * 2 * Math.PI));
            Cursor cursor = Plugin.context.getContentResolver().query(Provider.TableOne_Data.CONTENT_URI,
                        new String[] {Provider.TableOne_Data.LOCATION_NAME, Provider.TableOne_Data.LONGITUDE, Provider.TableOne_Data.LATITUDE, Provider.TableOne_Data.ACCURACY},
                            "(" + Provider.TableOne_Data.LATITUDE  + " BETWEEN " + down.getLatitude() + " AND " + up.getLatitude() + ") AND " +
                            "(" + Provider.TableOne_Data.LONGITUDE + " BETWEEN " + left.getLatitude() + " AND " + right.getLatitude() + ")",
//                            Provider.TableOne_Data.ACCURACY + "<" + accuracy,
                        null,
                        Provider.TableOne_Data.TIMESTAMP + " DESC LIMIT 1");
            return cursor;
        }
    }

    private ProviderManager provide = new ProviderManager(new Provider());

    //default constructor should be replaced with more useful constructor in future
    DataManager() {
        provide.start();
    }

    void giveLocation(final Context context, final Location location) {
        //TODO remove hardcoded insert
//        provide.addMarkedLocation(new MarkedLocation("hello world", location));
        if (isNoteworthy(location)) {
            onLocationReceive(context, location);
        }
    }

    private boolean isNoteworthy(Location location) {
        Log.i(TAG, "Checking if location is noteworthy");
        if (location.getSpeed() > 4)
            return false;
        if (previousLocation == null) {
            Log.i(TAG, "Not known previous location");
            previousLocation = location;
            return true;
        }
        if (location == null || location.getAccuracy() > TOLERABLE_ACCURACY || distance(location.getLatitude(), location.getLongitude(), previousLocation.getLatitude(),
                previousLocation.getLongitude()) < NEGLIGIBLE_RANGE) { //If points are within negligible range
            Log.i(TAG, "Location was negligible");
            return false;
        }
        previousLocation = location;
        return true;
    }

    private void onLocationReceive(Context context, Location location) {
        try {
            int locType = getLocationType(location);
            switch (locType) {
                case 0:
                    askNewLocation(context, location);
                    break;
                case 1:
                    askPreviousLocation(context, location);
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void askNewLocation(Context context, Location location) throws JSONException {
        ESM_Radio q1 = new ESM_Radio();
        ESM_Radio q2 = new ESM_Radio();
        ESMFactory factory1 = new ESMFactory();
        q1.addRadio("Work")
                .addRadio("Home")
                .addRadio("Café")
                .addRadio("Gym")
                .addRadio("Restaurant")
                .addRadio("Other")
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

    void askPreviousLocation(Context context, Location location) throws JSONException {
        ESM_Radio q1 = new ESM_Radio();
        ESM_Radio q2 = new ESM_Radio();
        ESMFactory factory1 = new ESMFactory();
        q1.addRadio("Working")
                .addRadio("Cooking")
                .addRadio("Studying")
                .addRadio("Leisure")
                .addRadio("Eating")
                .addRadio("Other")
                .setInstructions(PREV_QUESTION_1)
                .setTitle("Previous Location")
                .setSubmitButton("OK");
        factory1.addESM(q1);
        q2.addRadio("Alone")
                .addRadio("Family")
                .addRadio("Partner")
                .addRadio("Friends")
                .addRadio("Colleague")
                .addRadio("Other")
                .setInstructions(PREV_QUESTION_2)
                .setTitle("Previous Location")
                .setSubmitButton("OK");
        factory1.addESM(q2);
        questionsPerQueue = 2;
        ESM.queueESM(context, factory1.build());
        for (int i = 0; i < questionsPerQueue; i++)
            toBeAnswered.add(new Entry(location));
    }

    void onESMAnswered(JSONObject info, String answer) {
        if (info == null || answer == null)
            return;
        Entry entry = toBeAnswered.poll();
        storeData(info, answer, entry);
    }

    private int getLocationType(Location loc) {
        Cursor c = provide.getLocationsWithin(50, 50, loc);
        Log.i(TAG, "Number of nearby locations: " +c.getCount());

        if (c == null || c.getCount() == 0) {
            return 0;
        } else {
            return 2;
        }
    }

    private void storeData(JSONObject esmJson, String esmAnswer, Entry entry) {
        try {
            String instructions = esmJson.getString("esm_instructions");
            if (instructions.equals(NEW_QUESTION_1)){
                //Setting name value
                entry.put(entry.name,esmAnswer);
            }
            else if (instructions.equals(NEW_QUESTION_2)) {
                //Setting frequency value
                entry.put(entry.frequency,esmAnswer);
            }
            else if (instructions.equals(PREV_QUESTION_1)) {
                //Setting activity value
                entry.put(entry.activity,esmAnswer);
            }
            else if (instructions.equals(PREV_QUESTION_2)) {
                //Setting with value
                entry.put(entry.with,esmAnswer);
            }
            else
                return;
            provide.addEntry(entry);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Log.i(TAG, "\n-----------------------------------------\n" +
                    "Adding location to database:\nQuestion: "
                    + esmJson.getString("esm_instructions")
                    + "\nAnswer: "+ esmAnswer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        printEntry(entry);
        Log.i(TAG, "\n-----------------------------------------\n");
    }



    void onESMCancelled() {
        Log.i(TAG, "ESM cancelled");
        toBeAnswered.poll(); //Remove corresponding location
    }

    private void printEntry(Entry loc) {
        if (loc == null) {
            Log.i(TAG, "Location was null.");
            return;
        }
        Log.i(TAG, "\n" + loc.get(loc.lat) + ", " + loc.get(loc.lon)+
                "\nTime:" + loc.get(loc.time)+ "\nAccuracy: " + loc.get(loc.accuracy));
    }

    private int distance(double lat1, double lon1, double lat2, double lon2) {
        double p = 0.017453292519943295;    // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2 +
                Math.cos(lat1 * p) * Math.cos(lat2 * p) *
                        (1 - Math.cos((lon2 - lon1) * p)) / 2;

        return (int) (1000 * 12742 * Math.asin(Math.sqrt(a))); // 2 * R; R = 6371 km
    }
}

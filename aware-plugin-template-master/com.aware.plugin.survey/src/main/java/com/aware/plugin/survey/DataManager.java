package com.aware.plugin.survey;

import android.content.Context;
import android.database.Cursor;
import android.location.*;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.ESM;
import com.aware.ui.esms.*;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.ContentValues.TAG;

/*
 * Created by Nick on 23/03/2017.
 */

class DataManager {
    static final String NEW_QUESTION_1 = "Where are you?";
    private static final String NEW_QUESTION_2 = "How often do you come here?";
    static final String PREV_QUESTION_1 = "What type of activity are you doing?";
    private static final String PREV_QUESTION_2 = "Who are you doing this activity with?";
    static final String TIME_QUESTION_START = "When would you like questions to start?";
    static final String TIME_QUESTION_STOP = "When would you like questions to stop?";
    private static Location previousLocation = null;
    private int startQuestions = 0;
    private int stopQuestions = 0;
    static boolean timeSet = false; //Won't record locations until true
    private ConcurrentLinkedQueue<Entry> toBeAnswered = new ConcurrentLinkedQueue<>();
    static int questionsPerQueue;
    private boolean locationChecked = false;
    private Date asked = null; //When last question was asked

    private ProviderManager provider = new ProviderManager(new Provider());

    //default constructor should be replaced with more useful constructor in future
    DataManager() {
        provider.start();
    }

    void giveLocation(final Context context, final Location location) {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        if(hour<startQuestions || hour>stopQuestions) {
            Log.i(TAG, "Not within time limits");
            return;
        }
        if (timeSet) {
            if (isNoteworthy(location)) {
                onLocationReceive(context, location);
            }
        } else {
            Log.i(TAG, "---------------------------------\n " + DataManager.timeSet + "\n Times : " +
                    startQuestions + " : " + stopQuestions);
        }
    }

    private boolean isNoteworthy(Location location) {
        Log.i(TAG, "Checking if location is noteworthy");
        if (location == null || location.getSpeed() > 4 || location.getAccuracy() > 250)
            return false;
        if (previousLocation == null) {
            Log.i(TAG, "No previous location");
            previousLocation = location;
            return false;
        }
        if (location.getTime() - previousLocation.getTime() < 900000) { //Minimum time is 15 minutes
            Log.i(TAG, "Not at location long enough");
            return false;
        }
        int dist=distance(location.getLatitude(), location.getLongitude(), previousLocation.getLatitude(),
                previousLocation.getLongitude());
        if (!(dist < 30)) { //If points are not within negligible range*/
            Log.i(TAG, "Location changed by " +dist + ", resetting timer");
            locationChecked = false;
            return false;
        }
        if (locationChecked) {
            Log.i(TAG, "Location hasn't changed since last question");
            return false;
        }
        if (asked != null && new Date().getTime() - asked.getTime() < 3600000) {
            Log.i(TAG, "Not long enough since last question");
            return false;
        } //Only ask questions after an hour since last one
        locationChecked =true;
        asked = new Date();
        previousLocation = location;
        return true;
    }

    private void onLocationReceive(Context context, Location location) {
        try {
            Entry entry = isPreviousLocation(location);
            if (entry == null) {
                askNewLocation(context, location);
            } else {
                askPreviousLocation(context, entry);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private Entry isPreviousLocation(Location loc) {
        Entry[] entries = getEntries();
        Entry closest = null;
        int closestDist = Integer.MAX_VALUE;
        int dist;
        for (Entry e : entries) {
            dist = distance(Double.parseDouble(e.get(e.lat)), Double.parseDouble(e.get(e.lon))
                    , loc.getLatitude(), loc.getLongitude());
            Log.i(TAG, "Dist from e: " + dist + "\nE Name: " + e.get(e.name));
            if (dist < Integer.parseInt(e.get(e.range)) && dist < closestDist) {
                closest = e;
            }
        }
        if (closest == null) {
            Log.i(TAG, "Twas null");
            return null;
        } else if (closest.get(closest.name).equals("")) {
            return null;
        }

        provider.delete(closest.get(closest.name));
        return closest;
    }

    private Entry[] getEntries() {
        ArrayList<Entry> entriesList = new ArrayList<>();
        Cursor c = provider.getAll();
        if (!c.moveToFirst()) //Check if database is empty
            return new Entry[0];
        boolean end = false;
        while (!end) {
            Entry e = new Entry();
            //Change database to entry array
            e.put(e.name, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LOCATION_NAME)));
            e.put(e.lat, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LATITUDE)));
            e.put(e.lon, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LONGITUDE)));
            e.put(e.accuracy, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.ACCURACY)));
            e.put(e.time, e.longToString(c.getLong(c.getColumnIndex(Provider.Location_Survey_Table.TIMESTAMP))));
            e.put(e.frequency, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.FREQUENCY)));
            e.put(e.activity, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.ACTIVITY)));
            e.put(e.with, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.WITH)));
            entriesList.add(e);
            end = !c.moveToNext();
        }
        Entry[] entries = new Entry[entriesList.size()];
        for (int i = 0; i < entries.length; i++)
            entries[i] = entriesList.get(i);
        return entries;
    }
    /*
    To add questions:
    Create question and when set up add to factory.
    Change "questionsPerQueue" to the number of questions to be answered
     */
    private void askNewLocation(Context context, Location location) throws JSONException {
        ESM_Radio q1 = new ESM_Radio();
        ESM_Radio q2 = new ESM_Radio();
        ESMFactory factory1 = new ESMFactory();
        Entry[] entries = getEntries();
        for (Entry e : entries) q1.addRadio(e.get(e.name));
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
        for(Entry e: entries)
            printEntry(e);
        for (Entry e : entries)
            if (!e.get(e.activity).equals("null"))
                q1.addRadio(e.get(e.activity)); //Gets existing entries in database and displays as options
        q1.addRadio("Other") //Option to allow user to input new entry
                .setInstructions(PREV_QUESTION_1)
                .setTitle("Previous Location")
                .setSubmitButton("OK");
        factory.addESM(q1);
        for (Entry e : entries)
            if (!e.get(e.with).equals("null"))
                 q2.addRadio(e.get(e.with));
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


    void onESMAnswered(String answer1, String answer2, int type) {
        if (answer1 == null || answer2 == null || (toBeAnswered.isEmpty() && type != 3)) {
            Log.e(TAG, "Part of the ESM was null or the queue of locations was empty.");
            return;
        }
        Entry entry = toBeAnswered.poll();
        storeData(answer1, answer2, entry, type);
    }


    private void storeData(String answer1, String answer2, Entry entry, int type) {
        switch (type) {
            case 1:
                //Setting name value
                entry.put(entry.name, answer1);
                //Setting frequency value
                entry.put(entry.frequency, answer2);
                provider.addEntry(entry);
                break;
            case 2:
                //Setting activity value
                entry.put(entry.activity, answer1);
                //Setting with value
                entry.put(entry.with, answer2);
                provider.addEntry(entry);
                break;
            case 3:
                startQuestions = Integer.parseInt(answer1);
                stopQuestions = Integer.parseInt(answer2);
                Log.i(TAG, "Setting time to be between " + answer1 + ":00 and " + answer2 + ":00");
                break;
            default:
                Log.e(TAG, "Received answer to unknown ESM.\nClearing queue to be answered.");
                toBeAnswered.clear();
        }
    }

    /**
     * Functions below not directly in timeline of events
     */

    void onESMCancelled() {
        Log.i(TAG, "ESM cancelled");
        toBeAnswered.clear(); //Remove corresponding location
    }

    static void printEntry(Entry loc) {
        if (loc == null) {
            Log.i(TAG, "Location was null.");
            return;
        }
        Log.i(TAG, "Entry:\n" +
                "Time: " + loc.get(loc.time) + "\n" +
                "Name: " + loc.get(loc.name) + "\n" +
                "Lat: " + loc.get(loc.lat) + "\n" +
                "Lon: " + loc.get(loc.lon) + "\n" +
                "Accuracy: " + loc.get(loc.accuracy) + "\n" +
                "Range: " + loc.get(loc.range) + "\n" +
                "Frequency: " + loc.get(loc.frequency) + "\n" +
                "Activity: " + loc.get(loc.activity) + "\n" +
                "With: " + loc.get(loc.with) + "\n");
    }

    /**
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance between the two points based on the Haversine method
     */
    static int distance(double lat1, double lon1, double lat2, double lon2) {
        double p = 0.017453292519943295;    // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2 +
                Math.cos(lat1 * p) * Math.cos(lat2 * p) *
                        (1 - Math.cos((lon2 - lon1) * p)) / 2;

        return (int) (1000 * 12742 * Math.asin(Math.sqrt(a))); // 2 * R; R = 6371 km
    }
}

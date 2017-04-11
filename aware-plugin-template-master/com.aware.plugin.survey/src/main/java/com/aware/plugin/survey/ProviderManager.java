package com.aware.plugin.survey;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.ContentValues.TAG;

/**
 * Created by ronan on 11/04/2017.
 */

class ProviderManager extends Thread {

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

                values.put(Provider.Location_Survey_Table.LOCATION_NAME, data.values.get("name"));
                values.put(Provider.Location_Survey_Table.LATITUDE, Double.parseDouble(data.values.get("lat")));
                values.put(Provider.Location_Survey_Table.TIMESTAMP, Long.parseLong(data.values.get("time")));
                values.put(Provider.Location_Survey_Table.LONGITUDE, Double.parseDouble(data.values.get("lon")));
                values.put(Provider.Location_Survey_Table.ACCURACY, Double.parseDouble(data.values.get("accuracy")));
                values.put(Provider.Location_Survey_Table.RANGE, Integer.parseInt(data.values.get("range")));
                values.put(Provider.Location_Survey_Table.FREQUENCY, data.values.get("frequency"));
                values.put(Provider.Location_Survey_Table.ACTIVITY, data.values.get("activity"));
                values.put(Provider.Location_Survey_Table.WITH, data.values.get("with"));
                provider.insert(Provider.Location_Survey_Table.CONTENT_URI, values);
                Log.i(TAG, "Stored a location in the database.");
            }
        }
    }

    Cursor getForName(String name) {
        return Plugin.context.getContentResolver().query(
                Provider.Location_Survey_Table.CONTENT_URI,
                null,
                Provider.Location_Survey_Table.LOCATION_NAME + "='" + name + "'",
                null,
                Provider.Location_Survey_Table._ID + " DESC"
        );
    }

    void addEntry(Entry entry) {
        if (checkExists(entry.get(entry.name))) {
            Cursor c = getForName(entry.get(entry.name));
            c.moveToFirst();

            Entry e = new Entry();

            e.put(e.name, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LOCATION_NAME)));
            e.put(e.lat, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LATITUDE)));
            e.put(e.lon, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.LONGITUDE)));
            e.put(e.accuracy, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.ACCURACY)));
            e.put(e.time, e.longToString(c.getLong(c.getColumnIndex(Provider.Location_Survey_Table.TIMESTAMP))));
            e.put(e.frequency, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.FREQUENCY)));
            e.put(e.activity, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.ACTIVITY)));
            e.put(e.with, c.getString(c.getColumnIndex(Provider.Location_Survey_Table.WITH)));
            e.put(e.range, "" + DataManager.distance(entry.location.getLatitude(), entry.location.getLongitude(), Double.parseDouble(e.get(e.lat)), Double.parseDouble(e.get(e.lon))));
            toAdd.add(e);
            c.close();
        } else
            toAdd.add(entry);
    }

    Cursor getAll() {
        return Plugin.context.getContentResolver().query(Provider.Location_Survey_Table.CONTENT_URI,
                null,
                null,
                null,
                Provider.Location_Survey_Table.TIMESTAMP + " DESC");
    }

//        Cursor mostRecent() {
//            Cursor cursor = provider.query(Provider.Location_Survey_Table.CONTENT_URI, null, null, null, Provider.Location_Survey_Table.TIMESTAMP + " DESC 1");
//            cursor.moveToFirst();
//            return cursor;
//        }

//        Cursor getLocationsWithin(int metres, int accuracy, Location location) {
//            Location up, down, left, right;
//            up = new Location(location);
//            down = new Location(location);
//            left = new Location(location);
//            right = new Location(location);
//
//            up.setLatitude(up.getLatitude() + (((double) metres) / 111111.00));
//            down.setLatitude(up.getLatitude() - (((double) metres) / 111111.00));
//
//            left.setLongitude(left.getLongitude() - (((double) metres) / 111111.00) * Math.cos(left.getLatitude() * 2 * Math.PI));
//            right.setLongitude(right.getLongitude() + (((double) metres) / 111111.00) * Math.cos(right.getLatitude() * 2 * Math.PI));
//            Cursor cursor = Plugin.context.getContentResolver().query(Provider.Location_Survey_Table.CONTENT_URI,
//                    new String[]{Provider.Location_Survey_Table.LOCATION_NAME, Provider.Location_Survey_Table.LONGITUDE, Provider.Location_Survey_Table.LATITUDE},// Provider.Location_Survey_Table.ACCURACY},
//                    "(" + Provider.Location_Survey_Table.LATITUDE + " BETWEEN " + down.getLatitude() + " AND " + up.getLatitude() + ") AND " +
//                            "(" + Provider.Location_Survey_Table.LONGITUDE + " BETWEEN " + left.getLatitude() + " AND " + right.getLatitude() + ")",
////                            Provider.Location_Survey_Table.ACCURACY + "<" + accuracy,
//                    null,
//                    Provider.Location_Survey_Table.TIMESTAMP + " DESC LIMIT 1");
//            return cursor;
//        }
//
//        void printAllRows() {
//            Cursor cursor = Plugin.context.getContentResolver().query(Provider.Location_Survey_Table.CONTENT_URI,
//                    null,
//                    null,
//                    null,
//                    Provider.Location_Survey_Table.TIMESTAMP + " DESC LIMIT 1"
//            );
//            if (cursor == null) {
//                return;
//            }
//            Log.i(TAG, "here are all the rows of the database " + cursor.getCount());
//            while (cursor.moveToNext()) {
//                for (String column : cursor.getColumnNames()) {
//                    Log.i(TAG, "Column: " + column + " value " + cursor.getString(cursor.getColumnIndex(column)));
//                }
//            }
//            cursor.close();
//        }

    void delete(String name) {
        Plugin.context.getContentResolver().delete(
                Provider.Location_Survey_Table.CONTENT_URI,
                Provider.Location_Survey_Table.LOCATION_NAME + "='" + name + "'",
                null);
    }

    boolean checkExists(String locationName) {
        boolean exists;
        Cursor cursor = Plugin.context.getContentResolver().query(
                Provider.Location_Survey_Table.CONTENT_URI,
                null,
                "(" + Provider.Location_Survey_Table.LOCATION_NAME + "='" + locationName + "')",
                null,
                Provider.Location_Survey_Table._ID + " DESC"
        );
        if (cursor == null) {
            return false;
        }
        exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

}

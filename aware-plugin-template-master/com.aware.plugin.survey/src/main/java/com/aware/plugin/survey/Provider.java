package com.aware.plugin.survey;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.io.File;
import java.util.HashMap;

public class Provider extends ContentProvider {

    /*
        Despite the fact that these methods should work fine when called on a Provider object,
        We strongly recommend calling them via Context.getContentResolver().xxx() where xxx is for example query, insert or delete as needed.
     */


    //DATABASE CONSTANTS
    public static String AUTHORITY = "com.aware.provider.plugin.location.survey";
    public static final int DATABASE_VERSION = 7;
    public static final int Location_Survey = 1;
    public static final int Location_Survey_ID = 2;

    public static String DATABASE_NAME = "location_survey.db";

    private static String TAG = "DATABASE";

    //Table columns
    public static class Location_Survey_Table implements BaseColumns {
        private Location_Survey_Table() {}
        // Important init stuff
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/location_survey");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.location_survey";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.location_survey";

        public static final String _ID="_id";

        public static final String TIMESTAMP="timestamp";
        public static final String DEVICE_ID="device_id";

        //Actual columns
        public static final String LATITUDE = "latitude";
        public static final String ACTIVITY = "activity";
        public static final String LONGITUDE = "longitude";
        public static final String ACCURACY = "accuracy";
        public static final String LOCATION_NAME = "locaion_name";
        public static final String FREQUENCY = "location_frequency";
        public static final String WITH = "company";
        public static final String RANGE = "range";
        public static final String ESM_IDS = "esm_ids";
    }

    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/location_survey");
    public static final String[] DATABASE_TABLES = {"location_survey"};

    //Telling sql what type of columns they are
    public static final String[] TABLES_FIELDS = {
            Location_Survey_Table._ID + " integer primary key autoincrement, " +
                    Location_Survey_Table.TIMESTAMP + " real default 0," +
                    Location_Survey_Table.DEVICE_ID + " text default ''," +
                    Location_Survey_Table.LATITUDE + " real default 0," +
                    Location_Survey_Table.LONGITUDE + " real default 0," +
                    Location_Survey_Table.ACCURACY + " real default 0," +
                    Location_Survey_Table.LOCATION_NAME + " text default ''," +
                    Location_Survey_Table.FREQUENCY + " real default 0," +
                    Location_Survey_Table.WITH + " text default ''," +
                    Location_Survey_Table.ESM_IDS + " text default ''," +
                    Location_Survey_Table.RANGE + " real default 0," +
                    Location_Survey_Table.ACTIVITY + " text default''"
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> tableMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.plugin.example"; //make AUTHORITY dynamic
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], Location_Survey); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", Location_Survey_ID); //URI for a single record
        //added columns to the mapping
        tableMap = new HashMap<String, String>();
        tableMap.put(Location_Survey_Table._ID, Location_Survey_Table._ID);
        tableMap.put(Location_Survey_Table.TIMESTAMP, Location_Survey_Table.TIMESTAMP);
        tableMap.put(Location_Survey_Table.DEVICE_ID, Location_Survey_Table.DEVICE_ID);
        tableMap.put(Location_Survey_Table.LATITUDE, Location_Survey_Table.LATITUDE);
        tableMap.put(Location_Survey_Table.LONGITUDE,Location_Survey_Table.LONGITUDE);
        tableMap.put(Location_Survey_Table.ACCURACY, Location_Survey_Table.ACCURACY);
        tableMap.put(Location_Survey_Table.LOCATION_NAME, Location_Survey_Table.LOCATION_NAME);
        tableMap.put(Location_Survey_Table.FREQUENCY, Location_Survey_Table.FREQUENCY);
        tableMap.put(Location_Survey_Table.WITH, Location_Survey_Table.WITH);
        tableMap.put(Location_Survey_Table.ESM_IDS, Location_Survey_Table.ESM_IDS);
        tableMap.put(Location_Survey_Table.RANGE, Location_Survey_Table.RANGE);
        tableMap.put(Location_Survey_Table.ACTIVITY, Location_Survey_Table.ACTIVITY);
        return true; //let Android know that the database is ready to be used.
    }

    //initialize database to memory, do not change
    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen()) ) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }


    //reset the database
    public static void resetDB( Context c ) {
        Log.d("AWARE", "Resetting " + DATABASE_NAME + "...");

        File db = new File(DATABASE_NAME);
        db.delete();
        databaseHelper = new DatabaseHelper(c, DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (databaseHelper != null) {
            database = databaseHelper.getWritableDatabase();
        }
    }


    // delete an entry from the database
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(TAG,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case Location_Survey:
                count = database.delete(DATABASE_TABLES[0], selection,selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    //get the type of the particular uri
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case Location_Survey:
                return Location_Survey_Table.CONTENT_TYPE;
            case Location_Survey_ID:
                return Location_Survey_Table.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    //insert a value into the database
    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if( ! initializeDB() ) {
            Log.w(TAG,"Database unavailable...");
            return null;
        }

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
            case Location_Survey:
                long _id = database.insert(DATABASE_TABLES[0],Location_Survey_Table.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Location_Survey_Table.CONTENT_URI, _id);
                    Plugin.context.getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }


    //to query the database
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if( ! initializeDB() ) {
            Log.w(TAG,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case Location_Survey:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG) Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    //update a field
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(TAG,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case Location_Survey:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
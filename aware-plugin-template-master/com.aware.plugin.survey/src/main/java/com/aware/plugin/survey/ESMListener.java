package com.aware.plugin.survey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.aware.providers.ESM_Provider;
import com.aware.providers.Locations_Provider;

import static android.content.ContentValues.TAG;
/**
 * Created by ronan on 22/03/2017.
 */

public class ESMListener extends BroadcastReceiver {
    public ESMListener(){
        super();
    }

    public void onReceive(Context c, Intent intent) {
        Log.i(TAG,"An ESM was answered.");
        if (intent == null) {
            Log.i(TAG, "it was null");
        }
        else {
            Cursor data = c.getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, null, null, ESM_Provider.ESM_Data.TIMESTAMP + " DESC LIMIT 1");
            if (data != null && data.moveToFirst()) {
                Log.i(TAG," " + data.getString(data.getColumnIndex(ESM_Provider.ESM_Data.ANSWER)));
            }
            data.close();
            Log.i(TAG, "Location pinged");
        }
    }
}
package com.aware.plugin.survey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.aware.ESM;
import com.aware.providers.ESM_Provider;
import com.aware.providers.Locations_Provider;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;
/**
 * Created by ronan on 22/03/2017.
 */

public class ESMListener extends BroadcastReceiver {
    DataManager mgr;
    int answersReceived = 0;

    public ESMListener(DataManager mgr){
        super();
        this.mgr = mgr;
    }

    public void onReceive(Context c, Intent intent) {
        answersReceived++;
        if(answersReceived!=mgr.questionsPerQueue)
            return;
        answersReceived=0;
        Log.i(TAG, "An ESM was answered.");
        if (intent == null) {
            Log.i(TAG, "it was null");
        }
        else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)){
            try {
                final Cursor data = c.getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, null, null, ESM_Provider.ESM_Data.TIMESTAMP + " DESC LIMIT " + mgr.questionsPerQueue);
                if (data != null && data.moveToFirst()) {
                    for(int i=0;i<mgr.questionsPerQueue;i++) {
                        final JSONObject esmInfo = new JSONObject(data.getString(data.getColumnIndex(ESM_Provider.ESM_Data.JSON)));
                        final String s=data.getString(data.getColumnIndex(ESM_Provider.ESM_Data.ANSWER));
                        new Thread(new Runnable() {
                            public void run() {
                                mgr.onESMAnswered(esmInfo, s);
                            }
                        }).start();
                        data.moveToNext();
                    }
                }
                data.close();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Location pinged");
        }
        else{
            mgr.onESMCancelled();
        }
    }
}
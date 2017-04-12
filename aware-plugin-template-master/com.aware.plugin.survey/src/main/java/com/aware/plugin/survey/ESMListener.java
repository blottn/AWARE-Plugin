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
        if(answersReceived!=DataManager.questionsPerQueue)
            return;
        answersReceived=0;
        Log.i(TAG, "An ESM was answered.");
        if (intent == null) {
            Log.i(TAG, "it was null");
        }
        else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)){
            try {
                final Cursor data = c.getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, null, null, ESM_Provider.ESM_Data.TIMESTAMP + " DESC LIMIT " + mgr.questionsPerQueue);
                if (data != null && data.moveToLast()) {
                    JSONObject esmInfo = new JSONObject(data.getString(data.getColumnIndex(ESM_Provider.ESM_Data.JSON)));
                    String a1 = data.getString(data.getColumnIndex(ESM_Provider.ESM_Data.ANSWER));
                    data.moveToPrevious();
                    String a2 = data.getString(data.getColumnIndex(ESM_Provider.ESM_Data.ANSWER));
                    String instructions = esmInfo.getString("esm_instructions");
                    int type = (instructions.equals(DataManager.NEW_QUESTION_1)) ? 1 : instructions.equals(DataManager.PREV_QUESTION_1) ? 2 : 3;
                    int id = data.getInt(data.getColumnIndex(ESM_Provider.ESM_Data._ID));
                    if (type != 3) {
                        mgr.onESMAnswered(a1, a2, type, id);
                    } else {
                        String[] part1 = a1.split(":");
                        String[] part2 = a2.split(":");
                        mgr.onESMAnswered(part1[0], part2[0], type, id);
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
package com.aware.plugin.survey;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.Locations;
import com.aware.providers.Locations_Provider;
import com.aware.ui.PermissionsHandler;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Checkbox;
import com.aware.ui.esms.ESM_Freetext;
import com.aware.ui.esms.ESM_Question;
import com.aware.ui.esms.ESM_Radio;
import com.aware.utils.Aware_Plugin;

import org.json.JSONException;

import static android.content.ContentValues.TAG;

public class Plugin extends Aware_Plugin {
    static LocationListener listener = new LocationListener();
    private static boolean done=false;
    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::"+getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        // set up listener
        IntentFilter filter = new IntentFilter();
        filter.addAction(Locations.ACTION_AWARE_LOCATIONS);
        registerReceiver(listener, filter);
        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.TableOne_Data.CONTENT_URI }; //this syncs dummy TableOne_Data to server

        //Activate plugin -- do this ALWAYS as the last thing (this will restart your own plugin and apply the settings)
        Aware.startPlugin(this, "com.aware.plugin.survey");
    }

    public static void onLocationReceive(Context context, Intent intent){
        if(done) return;
        done=true;
        try {
            ESM_Radio question = new ESM_Radio();
            question.addRadio("Work")
                    .addRadio("Home")
                    .addRadio("Shopping")
                    .addRadio("Holiday")
                    .addRadio("Other")
                    .setTitle("Location Survey Questionnaire")
                    .setSubmitButton("OK")
                    .setInstructions("Where were you at 3:30pm");
            ESMFactory factory = new ESMFactory();
            factory.addESM(question);
            ESM.queueESM(context,factory.build());
        } catch (JSONException e) {
            e.printStackTrace();
        }
     }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok){
            //Check if the user has toggled the debug messages
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, true);

            //Ask AWARE to start ESM
            Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
            Aware.startESM(this);
            onLocationReceive(this,intent);

        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, false);

        //Stop AWARE's instance running inside the plugin package
        Aware.stopAWARE();
    }
}

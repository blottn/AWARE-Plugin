package com.aware.plugin.survey;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.Locations;
import com.aware.ui.PermissionsHandler;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Radio;
import com.aware.utils.Aware_Plugin;

import org.json.JSONException;


public class Plugin extends Aware_Plugin {

    static Context context;

    static DataManager default_mgr = new DataManager();
    static LocationListener locLis = new LocationListener(default_mgr);
    static ESMListener esmLis = new ESMListener(default_mgr);
    @Override
    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
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

        // set up listeners
        IntentFilter locationFilter = new IntentFilter();
        locationFilter.addAction(Locations.ACTION_AWARE_LOCATIONS);
        registerReceiver(locLis, locationFilter);

        IntentFilter esmFilter = new IntentFilter();
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        registerReceiver(esmLis, esmFilter);



        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Location_Survey_Table.CONTENT_URI };

        //Activate plugin -- do this ALWAYS as the last thing (this will restart your own plugin and apply the settings)
        Aware.startPlugin(this, "com.aware.plugin.survey");
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
            if (!DataManager.timeSet) {
                DataManager.timeSet = true;
                try {
                    ESM_Radio q1 = new ESM_Radio();
                    ESM_Radio q2 = new ESM_Radio();
                    ESMFactory factory = new ESMFactory();
                    for (int i=0;i<24;i=i+2)
                        q1.addRadio(i+":00"); //Gets existing entries in database and displays as options
                    q1.setInstructions(DataManager.TIME_QUESTION_START)
                            .setTitle("Set Start Time")
                            .setSubmitButton("OK");

                    factory.addESM(q1);
                    for (int i=0;i<24;i=i+2)
                        q2.addRadio(i+":00");
                    q2.setInstructions(DataManager.TIME_QUESTION_STOP)
                            .setTitle("Set End Time")
                            .setSubmitButton("OK");
                    factory.addESM(q2);
                    DataManager.questionsPerQueue = 2;
                    ESM.queueESM(context, factory.build());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

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
        unregisterReceiver(esmLis);
        unregisterReceiver(locLis);
        Aware.setSetting(this, Settings.STATUS_SURVEY_PLUGIN, false);
        //Stop AWARE's instance running inside the plugin package
        Aware.stopAWARE(this);
    }
}

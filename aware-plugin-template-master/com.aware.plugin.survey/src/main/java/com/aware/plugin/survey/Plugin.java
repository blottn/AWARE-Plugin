package com.aware.plugin.survey;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.ui.PermissionsHandler;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Likert;
import com.aware.ui.esms.ESM_PAM;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

public class Plugin extends Aware_Plugin {

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

        if (permissions_ok) {
            //Check if the user has toggled the debug messages
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

            //Ask AWARE to start ESM
            Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
            Aware.startESM(this);

            //Setting morning question
            try {
                //Using PAM to get users' affect state in the morning
                ESMFactory esmFactory = new ESMFactory();

                ESM_PAM morning_question = new ESM_PAM();
                morning_question.setTitle("Morning!")
                        .setInstructions("How are you feeling today?")
                        .setExpirationThreshold(0) //no expiration = shows a notification the user can use to answer at any time
                        .setNotificationTimeout(5 * 60) //the notification is automatically removed and the questionnaire expired after 5 minutes ( 5 * 60 seconds)
                        .setSubmitButton("OK");

                esmFactory.addESM(morning_question);

                //Schedule this question for the morning, only if not yet defined
                Scheduler.Schedule morning = Scheduler.getSchedule(this, "morning_question");
                if (morning == null) {
                    morning = new Scheduler.Schedule("morning_question"); //schedule with morning_question as ID
                    morning.addHour(8); //8 AM (24h format), every day
                    morning.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
                    morning.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
                    morning.addActionExtra(ESM.EXTRA_ESM, esmFactory.build()); //add the questions from the factory

                    Scheduler.saveSchedule(this, morning); //save the questionnaire and schedule it
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Setting evening question
            try {
                //Using Likert scale to get users' rating of the day
                ESMFactory esmFactory = new ESMFactory();

                ESM_Likert evening_question = new ESM_Likert();
                evening_question.setLikertMax(5)
                        .setLikertMinLabel("Awful")
                        .setLikertMaxLabel("Awesome!")
                        .setLikertStep(1)
                        .setTitle("Evening!")
                        .setInstructions("How would you rate today?")
                        .setExpirationThreshold(0) //no expiration = shows a notification the user can use to answer at any time
                        .setNotificationTimeout(5 * 60) //the notification is automatically removed and the questionnaire expired after 5 minutes ( 5 * 60 seconds)
                        .setSubmitButton("OK");

                esmFactory.addESM(evening_question);

                //Schedule this question for the evening, only if not yet defined
                Scheduler.Schedule evening = Scheduler.getSchedule(this, "evening_question");
                if (evening == null) {
                    evening = new Scheduler.Schedule("evening_question"); //schedule with morning_question as ID
                    evening.addHour(20); //8 PM (24h format), every day
                    evening.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
                    evening.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
                    evening.addActionExtra(ESM.EXTRA_ESM, esmFactory.build()); //add the questions from the factory

                    Scheduler.saveSchedule(this, evening); //save the questionnaire and schedule it
                }
            } catch (JSONException e) {
                e.printStackTrace();
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

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE's instance running inside the plugin package
        Aware.stopAWARE();
    }
}

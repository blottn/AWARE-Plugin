package com.aware.plugin.survey;

/**
 * Created by Nick on 22/03/2017.
 */

public class LocationPing {

    public double latitude; //latitude in degrees
    public double longitude; // longitude in degrees
    public long timestamp;   // unix timestamp in ms
    public int accuracy;    //accuracy in metres

    public LocationPing() {
        latitude = 0;
        longitude = 0;
        timestamp = 0;
        accuracy = 0;
    };

    @Override
    public String toString() {
        return this.timestamp + " " + this.latitude + " " + this.longitude + " " + this.accuracy;
    }

}

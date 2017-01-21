package com.example.sgrasu.mountainmetrics;

/**
 * Created by sgras on 11/22/2016.
 */

public class SensorRecord {
    public long date;
    public float  oxySat;
    //constructor added by me
    public SensorRecord(long d, float sat){
        date = d;
        oxySat=sat;}
}

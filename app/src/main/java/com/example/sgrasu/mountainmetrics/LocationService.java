package com.example.sgrasu.mountainmetrics;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;

import java.util.Calendar;
import java.util.Date;

import static android.R.attr.name;

public class LocationService extends Service implements android.location.LocationListener{
    private LocationManager locationManager;
    private String name;
    private int attemptCount;
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        name = extras.getString("name");
        attemptCount = 0;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,(LocationService) this);
            Log.e("LocationService------","requesting updates" );
        }
        catch(SecurityException s){
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void killService(){
        stopSelf();
    }
    @Override
    public void onLocationChanged(Location location){
        attemptCount++;
        if (location.getAccuracy()<100f && location.getAccuracy()!=0) {
            new LocationLoggerTask().execute(location);
            try{
                locationManager.removeUpdates(this);
            }
            catch(SecurityException s){
                stopSelf();
            }
        }
        else if(attemptCount>30){
            try{
                locationManager.removeUpdates(this);
                stopSelf();
            }
            catch(SecurityException s){
                stopSelf();
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class LocationLoggerTask extends
            AsyncTask<Location, Void, Void> {
        @Override
        protected Void doInBackground(Location... locations) {

            Location location = locations[0];
            Date time = Calendar.getInstance().getTime();
            long seconds = time.getTime() / 1000;
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
            SQLiteDatabase database = dbHelper.getWritableDatabase();
            ContentValues initialValues = new ContentValues();
            initialValues.put("lat", lat);
            initialValues.put("lng",lng);
            initialValues.put("time", seconds);

            Log.e("LocService latlng------","lat: "+lat+" lng:"+lng );

            database.insert(name, null, initialValues);
            killService();
            return null;
            }
        }
}
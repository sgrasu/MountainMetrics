package com.example.sgrasu.mountainmetrics;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by sgras on 11/29/2016.
 * adapted from https://code.tutsplus.com/tutorials/android-barometer-logger-acquiring-sensor-data--mobile-10558
 */

public class BaroLoggerService extends Service implements SensorEventListener {
    private static final String DEBUG_TAG = "BaroLoggerService";
    private Context appContext;
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private String name;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        name = extras.getString("name");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(this, sensor,
                SensorManager.SENSOR_DELAY_NORMAL);


        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // grab the values and timestamp
        new SensorEventLoggerTask().execute(event);
        // stop the sensor and service
        sensorManager.unregisterListener(this);
        stopSelf();
    }
    private class SensorEventLoggerTask extends
            AsyncTask<SensorEvent, Void, Void> {
        @Override
        protected Void doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];
            Date time = Calendar.getInstance().getTime();
            long seconds = time.getTime() / 1000;
            float pressure = event.values[0];
            int alt = (int) ((1 - Math.pow(pressure / 1013.25, 0.190284)) * 145366.45);
           DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
            SQLiteDatabase database = dbHelper.getWritableDatabase();
            ContentValues initialValues = new ContentValues();
            initialValues.put("alt", alt);
            initialValues.put("time", seconds);
            database.insert(name, null, initialValues);
            return null;
        }
    }

}
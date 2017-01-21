package com.example.sgrasu.mountainmetrics;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * Created by sgras on 11/22/2016.
 */

public class OxySatLoggerTask  {

    public interface Callback {
        void accessStart();
        void accessFinished(long time, float oxySat);
        void accessCancel(String url);
    }
    protected OxySatLoggerTask.Callback callback = null;
    protected Context cont;

    public OxySatLoggerTask(OxySatLoggerTask.Callback callback, Context c) {
        this.callback = callback;
        new AsyncSensorAccess().execute(c);
    }

    //AsyncTask is used to get oxygen saturation (currently just generating it randomly since
    //Samsung has not yet emailed me the SDK). Finally the data is input in the database
    //and callback is called so that the information can be plotted live in the OneExcursion Acrivity
    public class AsyncSensorAccess extends AsyncTask<Context, Integer, SensorRecord> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            callback.accessStart();
        }

        protected SensorRecord doInBackground(Context ... contexts) {
            Context context = contexts[0];
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            Date time = Calendar.getInstance().getTime();
            long seconds = time.getTime() / 1000;
            SQLiteDatabase database = dbHelper.getWritableDatabase();
            float newOxy = new Random().nextFloat() * 4 + 96;
            return new SensorRecord(seconds, newOxy);
        }
        protected void onPostExecute(SensorRecord result){
            callback.accessFinished(result.date,result.oxySat);
        }
    }
}


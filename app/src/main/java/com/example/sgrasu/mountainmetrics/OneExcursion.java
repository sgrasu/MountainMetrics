package com.example.sgrasu.mountainmetrics;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by sgras on 11/9/2016.
 */

public class OneExcursion extends AppCompatActivity implements OxySatLoggerTask.Callback, MapHolder.MapHolderCallback{

    private SupportMapFragment mapFragment;
    private MapHolder mapHolder;
    private String name;        //name of the Excursion
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private TextView tracker;
    private boolean oxy_enabled;    //used to prevent user from logging oxygen saturation
                                    //more than once per minute
    private LineChart chart;
    private long referenceTimestamp; //Used as reference when charting data so
                                    //that the earilest time charted is 0
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            //for sharing to facebook
            FacebookSdk.sdkInitialize(getApplicationContext());

            //gets name from main activity
            Intent activityThatCalled = getIntent();
            Bundle callingBundle = activityThatCalled.getExtras();
            name = callingBundle.getString("name");
            setTitle(name.replace('_',' '));

            referenceTimestamp = 0;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.excursion);
            dbHelper = new DatabaseHelper(getApplicationContext());
            db = dbHelper.getWritableDatabase();
            oxy_enabled = true;

            tracker = (TextView)findViewById(R.id.tracker);
            if(checkIfPendingIntentIsRegistered()) tracker.setText("Tracking: Yes");
            else tracker.setText("Tracking: no");


            chart = (LineChart) findViewById(R.id.chart);
            List<Entry> entries = new ArrayList<Entry>();
            chart.setData(new LineData());

            //to format dates from milliseconds to readable format (ii.e. Mar 6, 12:23 am)
            final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a");
            LineData data = null;
            Cursor cursor = dbHelper.getAllOxySats(name);
            int time_col = cursor.getColumnIndex("time");
            int alt_col = cursor.getColumnIndex("alt");
            int oxy_col = cursor.getColumnIndex("oxy_sat");

            //get all oxygen saturations in database and add them to the chart
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                if(i == 0) {
                    data = chart.getData();
                    ILineDataSet oxySet = data.getDataSetByLabel("oxygen saturation",true);
                    if (oxySet == null) {
                        oxySet = createOxySet();
                        data.addDataSet(oxySet);
                    }
                    if(referenceTimestamp == 0 || (referenceTimestamp<cursor.getLong(time_col) &&referenceTimestamp != 0))
                        referenceTimestamp = cursor.getLong(time_col);
                }ILineDataSet set = data.getDataSetByLabel("oxygen saturation", true);
                 set.addEntry(new Entry((float)cursor.getLong(time_col)-referenceTimestamp,(float)cursor.getDouble(oxy_col)));
                cursor.moveToNext();
            }


            //get all altitudes and plot them on the chart
            cursor = dbHelper.getAllAltitudes(name);
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                if(i == 0) {
                    data = chart.getData();
                    ILineDataSet altSet = data.getDataSetByLabel("altitude",true);
                    if (altSet == null) {
                        altSet = createAltSet();
                        data.addDataSet(altSet);
                    }
                    if(referenceTimestamp == 0 || (referenceTimestamp<cursor.getLong(time_col) &&referenceTimestamp != 0))
                        referenceTimestamp = cursor.getLong(time_col);
                }
                ILineDataSet set = data.getDataSetByLabel("altitude", true);
                set.addEntry(new Entry((float)cursor.getLong(time_col)-referenceTimestamp,(float)cursor.getDouble(alt_col)));
                cursor.moveToNext();
            }

            //refresh chart after adding data
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();

            //sets the formatters for the x axis and two y axes (sat & alt)
            chart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                        Date d = new Date((((long) value)+referenceTimestamp)*1000);
                        return (sdf.format(d));
                }
            });
            chart.getAxisLeft().setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return String.format("%.1f",value)+"%";
                }
            });
            chart.getAxisRight().setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                    return (numberFormat.format(((int) value))+ " ft");
                }
            });

            //chart formatting options
            YAxis rightYAxis = chart.getAxisRight();
            rightYAxis.setDrawGridLines(false);
            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(7f);
            xAxis.setTextColor(Color.RED);
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(false);
            xAxis.setLabelRotationAngle(30);
            Legend legend = chart.getLegend();
            legend.setMaxSizePercent(0.2f);
            Description desc = new Description();
            desc.setText("");
            chart.setDescription(desc);
            chart.setNoDataText("No Data recorded yet");
            chart.invalidate();

            final Button oxyBut = (Button) findViewById(R.id.oxy_but);
            oxyBut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!oxy_enabled){
                        Toast.makeText(getApplicationContext(),"Must wait 1 minute between sat. measurements",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        startOxySatTask();
                        Button myButton = (Button) v;
                        oxy_enabled = false;
                        myButton.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                oxy_enabled = true;
                            }
                        }, 60000);
                    }
                }
            });

            //setting up start and stop buttons for services
            Button startService = (Button)findViewById(R.id.serv_start);
            Button endService = (Button)findViewById(R.id.serv_end);
            startService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(),"starting location and altitude tracking",Toast.LENGTH_SHORT).show();
                    AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(getApplicationContext(), BaroLoggerService.class );
                    Intent locIntent = new Intent(getApplicationContext(),LocationService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("name",name);
                    int id = name.hashCode();
                    intent.putExtras(bundle);
                    locIntent.putExtras(bundle);
                    PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    PendingIntent scheduledLocIntent = PendingIntent.getService(getApplicationContext(), id, locIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, scheduledIntent);
                    scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HALF_HOUR, scheduledLocIntent);
                    tracker.setText("Tracking: Yes");

                }
            });
            endService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(),"ending location and altitude tracking",Toast.LENGTH_SHORT).show();
                    AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(getApplicationContext(), BaroLoggerService.class );
                    Intent locIntent = new Intent(getApplicationContext(),LocationService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("name",name);
                    int id = name.hashCode();
                    intent.putExtras(bundle);
                    locIntent.putExtras(bundle);
                    PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    PendingIntent scheduledLocIntent = PendingIntent.getService(getApplicationContext(), id, locIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    scheduler.cancel(scheduledIntent);
                    scheduler.cancel(scheduledLocIntent);
                    scheduledIntent.cancel();
                    scheduledLocIntent.cancel();
                    tracker.setText("Tracking: no");
                }
            });

        //Deal with map
            mapFragment = new SupportMapFragment();
            mapHolder = new MapHolder(this.getApplicationContext(),this);
            mapFragment.getMapAsync(mapHolder);
            if(savedInstanceState == null){
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.map_container,mapFragment).commit();
            }
        }
    public void onMapHolderReady(){

        //converts all locations in database to markers
         Cursor cursor = dbHelper.getAllLatLngs(name);
        cursor.moveToFirst();
        int latCol = cursor.getColumnIndex("lat");
        int lngCol = cursor.getColumnIndex("lng");
        mapHolder.reserMarkers();
        for (int i = 0; i < cursor.getCount(); i++) {
                double lat = cursor.getDouble(latCol);
                double lng = cursor.getDouble(lngCol);
            mapHolder.addMarker(new LatLng(lat,lng));
            cursor.moveToNext();
        }
    }

    //oxygen saturation dataset initialization and formatting
    private LineDataSet createOxySet() {

        LineDataSet set = new LineDataSet(null, "oxygen saturation");
        set.setLineWidth(2.5f);
        set.setCircleRadius(3.5f);
        set.setColor(Color.rgb(240, 99, 99));
        set.setCircleColor(Color.rgb(240, 99, 99));
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(4f);

        return set;
    }

    //altitude dataset initialization and formatting
    private LineDataSet createAltSet() {

        LineDataSet set = new LineDataSet(null, "altitude");
        set.setLineWidth(2.5f);
        set.setCircleRadius(4.5f);
        set.setColor(Color.rgb(129, 150, 55));
        set.setCircleColor(Color.rgb(129, 150, 55));
        set.setHighLightColor(Color.rgb(60, 60, 60));
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setValueTextSize(4f);

        return set;
    }
    public void accessStart (){}

    //callback function for asynctask that gets oxygen saturation.
    //is used for live plotting of oxygen-sat data
    public void accessFinished(long seconds, float saturation){
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        initialValues.put("oxy_sat", saturation);
        initialValues.put("time", seconds);
        database.insert(name, null, initialValues);
        LineData data = chart.getData();
        ILineDataSet set = data.getDataSetByLabel("oxygen saturation", true);

        if (set == null) {
            set = createOxySet();
            data.addDataSet(set);
            if(referenceTimestamp == 0 || (referenceTimestamp<seconds &&referenceTimestamp != 0))
                referenceTimestamp = seconds;
        }
        set.addEntry(new Entry((float) seconds - referenceTimestamp, saturation));
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.fitScreen();
        chart.invalidate();
    }
    public void accessCancel(String unused){}
    public void startOxySatTask(){
        new OxySatLoggerTask(this,getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_share, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.action_share:
                onShareClicked();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //share to facebook
    public void onShareClicked(){
        if(!isNetworkAvailable()){
            Toast.makeText(getApplicationContext(),"Sorry cannot share, no network connection",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        final ShareDialog shareDialog = new ShareDialog(this);
        if(shareDialog.canShow(ShareLinkContent.class)){
            //Bitmap shareScreen = getScreenShot(getWindow().getDecorView().findViewById(android.R.id.content));

             //shareDialog.show(content);
            final View view = this.getWindow().getDecorView();
            view.setDrawingCacheEnabled(true);

            //bmap contains screenshot of this excursion. map appears blank since the
            //map fragment uses OpenGL. Therefore a snapshot must be grabbed from the
            //map and then overlaid on the screenshot in bmap
            final Bitmap bmap = view.getDrawingCache();


            //callback that is called when the map returns a snapshot. The snapshot is
            //overlaid on bmap and then then passed to a facebook ShareDialog to send
            //to the facebook app.
            GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                Bitmap bitmap;

                @Override
                public void onSnapshotReady(Bitmap snapshot) {
                    Canvas back = new Canvas(bmap);
                    Canvas front = new Canvas(snapshot);
                    back.drawBitmap(snapshot,getRelativeLeft(findViewById(R.id.map_container)),
                            getRelativeTop(findViewById(R.id.map_container))+320,null);
                    Bitmap finalBitmap = Bitmap.createBitmap(back.getWidth(), back.getHeight(), Bitmap.Config.RGB_565);
                    back.setBitmap(finalBitmap);
                    SharePhoto photo = new SharePhoto.Builder().setBitmap(bmap)
                            .setCaption("Look at my latest trip to "+name.replace('_',' '))
                            .build();
                    SharePhotoContent content = new SharePhotoContent.Builder()
                            .addPhoto(photo)
                            .build();
                    shareDialog.show(content);
                }
            };
            mapHolder.getSnapshot(callback);
        }
    }

    //from http://stackoverflow.com/questions/3619693/getting-views-coordinates-relative-to-the-root-layout
    //used in the googlemap.snapshotcallback find position in screenshot on which
    //to overlay the snapshot
    private int getRelativeLeft(View myView) {
            return myView.getLeft();
    }

    private int getRelativeTop(View myView) {
            return myView.getTop();
    }
    //ensures network connection available, specifically for sharing to facebook
    // from http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //adapted from http://stackoverflow.com/questions/13292085/get-list-of-registered-pending-intents-in-android-os
    //checks to see if there is currently a pendingintent for the barometer service. If barometer service has a
    //pending intent in the alarm manager then so does the location service.
    private boolean checkIfPendingIntentIsRegistered() {
        Intent intent = new Intent(getApplicationContext(), BaroLoggerService.class );
        int id = name.hashCode();
        PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), id, intent, PendingIntent.FLAG_NO_CREATE);
        return (scheduledIntent != null);
    }
}

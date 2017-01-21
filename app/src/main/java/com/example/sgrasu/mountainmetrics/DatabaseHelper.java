package com.example.sgrasu.mountainmetrics;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Created by Grasu on 11/8/16. using flippedClassroom
 * as basis
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "excursions.db";
    private static final String TABLE_EXCURSIONS = "excursions_table";
    private static final String KEY_ID= "_id";
    private static final String KEY_NAME="name";
    private static final String KEY_DATE="creation_date";
    private static String DB_PATH;
    private SQLiteDatabase db;
    private final Context context;
    private static int DB_VERSION = 1;
    private static final String DATABASE_CREATE =
            "create table if not exists logs (_id integer primary key autoincrement,date text, city text, type text, log text, nick text);";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 2);
        DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
        this.context = context;
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String sql = "CREATE TABLE " + TABLE_EXCURSIONS + " ("+
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_NAME+ " TEXT, "+
                KEY_DATE + " INTEGER)";
        sqLiteDatabase.execSQL(sql);
        Log.e("DB","creating database");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABL IF EXISTS excursions_table");
    }
    public void insertExcursion(String name){
        Log.e("insertexcurs", name);

        name = name.replace(' ','_');
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        Log.e("datetime",""+Calendar.getInstance().getTime().getTime());
        initialValues.put(KEY_DATE, Calendar.getInstance().getTime().getTime());

        String sql = "CREATE TABLE " + name + " ("+
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                " oxy_sat "+ " REAL, " +
                "alt " + "INTEGER, " +
                "lat " + "REAL, " +
                "lng " + "REAL, " +
                "time INTEGER)";
        db.execSQL(sql);
        db.insert(TABLE_EXCURSIONS,null,initialValues );
    }
    public void deleteAllExcursions(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABLE_EXCURSIONS, null);
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            db.execSQL("DROP TABLE IF EXISTS " + cursor.getString(cursor.getColumnIndex(KEY_NAME)));

            cursor.moveToNext();
        }
        db.execSQL("delete from "+ TABLE_EXCURSIONS);

    }
    private void copyDatabase() throws IOException {
        InputStream input = context.getAssets().open(DB_NAME);
        String outFileName = DB_PATH + DB_NAME;
        OutputStream output = new FileOutputStream(outFileName);

        byte[] buf = new byte[4096];
        int len;
        while ((len = input.read(buf)) > 0) {
            output.write(buf, 0, len);
        }
        output.flush();
        output.close();
        input.close();
    }

    private boolean checkDatabase() {
        Log.e("DB","test logs");
        SQLiteDatabase checkDB = null;
        boolean exist = false;
        try {
            String path = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            Log.e("DB", "Database doesn't exist");
        }

        if (checkDB != null) {
            checkDB.close();
            exist = true;
        }
        return exist;
    }

    public void createDatabase() throws IOException {
        boolean exist = checkDatabase();

        if (!exist) {
            this.getReadableDatabase();
            this.close();
            try {
                copyDatabase();
            } catch (IOException e) {
                throw new IOException("Fail to copy database");
            }
        }
    }

    @Override
    public synchronized void close() {
        if (db != null)
            db.close();
        super.close();
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }
    public Cursor getAllAltitudes(String name){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + name + " WHERE alt IS NOT NULL", null);
    }
    public Cursor getAllOxySats(String name){
        //Log.e("db name", name);
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + name + " WHERE oxy_sat IS NOT NULL", null);
    }
    public Cursor getAllLatLngs(String name){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + name + " WHERE lat IS NOT NULL AND lng IS NOT NULL", null);
    }
}

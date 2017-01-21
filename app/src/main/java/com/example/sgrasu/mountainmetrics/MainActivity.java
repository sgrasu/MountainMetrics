package com.example.sgrasu.mountainmetrics;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private SQLiteDatabase excursionDb;
    private DatabaseHelper dbHelper;
    private ExcursionItemAdapter excursionAdapter;
    private ListView excursions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.home_view);
        final EditText editText = (EditText) findViewById(R.id.create_excursion);
        dbHelper = new DatabaseHelper(getApplicationContext());
        try {
            dbHelper.createDatabase();
        } catch (IOException e) {
            Log.e("DB", "Fail to create database");
        }
        initExcursionDB();

        //Allows soft keyboard to disappear and reappear when focus is not on the edittext
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){

                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(v.getWindowToken(),0);
                }
                else{
                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                            .showSoftInput(v,0);
                }
            }
        });


        //Listens for user to press enter and begins to create a new excursion
        editText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            String input = editText.getText().toString();

                            //replaces  spaces to comply with sqlite rules for
                            //table entries and names
                            input = input.replace(' ','_');
                            try {
                                Log.e("listener", input);
                                addExcursion(input);
                                Cursor cursor = excursionDb.rawQuery("select * from excursions_table", null);
                                excursionAdapter.changeCursor(cursor);
                                ((EditText) v).setText(null);
                                return true;
                            }
                            catch (SQLiteException e){
                                //ensures SQLite failure does not kill app
                                Toast.makeText(getApplicationContext(),"name not allowed, possibly a repeat",Toast.LENGTH_SHORT).show();
                            }
                        default:
                            break;
                    }
                }
                return false;
            }
        });
    }

    private void initExcursionDB() {
        excursions = (ListView) findViewById(R.id.excursions);
        excursionDb = dbHelper.getReadableDatabase();
        excursionAdapter = new ExcursionItemAdapter(this, null, false);
        Cursor cursor = excursionDb.rawQuery("select * from excursions_table", null);
        excursions.setAdapter(excursionAdapter);
        excursionAdapter.changeCursor(cursor);
    }

    private void addExcursion(String name) {
        Log.d("addex", name);
        dbHelper.insertExcursion(name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                onOpenSettingsClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onOpenSettingsClick() {
        Intent openSettings = new Intent(this, Settings.class);
        final int result = 1;
        startActivityForResult(openSettings, result);
    }

    //handles return from settings. Updates cursor in case user decided
    //to delete all excursions
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("return Code", "" + resultCode);
        if(resultCode == 1){
            Cursor cursor = excursionDb.rawQuery("select * from excursions_table", null);
            excursionAdapter.changeCursor(cursor);
        }
    }
}

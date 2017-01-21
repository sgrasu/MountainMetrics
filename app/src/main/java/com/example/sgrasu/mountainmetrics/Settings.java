package com.example.sgrasu.mountainmetrics;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * Created by sgras on 11/15/2016.
 */
public class Settings extends AppCompatActivity {
    Button deleteEntriesBut;
    DatabaseHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        Intent activityThatCalled = getIntent();
        Bundle callingBundle = activityThatCalled.getExtras();
        dbHelper = new DatabaseHelper(this);
        deleteEntriesBut = (Button) findViewById(R.id.delete_entries_but);
        deleteEntriesBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.deleteAllExcursions();
                setResult(1,null);
                finish();
            }
        });
    }

}

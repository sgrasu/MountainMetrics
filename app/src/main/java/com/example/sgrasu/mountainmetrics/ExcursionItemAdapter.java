package com.example.sgrasu.mountainmetrics;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Stefan Grasu, inspired by class code .
 */
public class ExcursionItemAdapter extends CursorAdapter {

    public ExcursionItemAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.excursion_item, viewGroup, false);
    }

    @Override
    public void bindView(final View view, Context context, Cursor cursor) {
        final TextView excursionName = (TextView) view.findViewById(R.id.excursion_name);
        final TextView excursionDate = (TextView) view.findViewById(R.id.excursion_date);
        String name = cursor.getString(cursor.getColumnIndex("name"));
        long seconds = cursor.getLong(cursor.getColumnIndex("creation_date"));
        Log.e("datetime",""+ seconds);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy");
        Date date = new Date((seconds));

        excursionName.setText(name.replace('_',' '));
        excursionDate.setText(sdf.format(date));

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openExcursion = new Intent(v.getContext(),OneExcursion.class);
                Bundle myExtras = new Bundle();
                myExtras.putString("name", excursionName.getText().toString().replace(" ","_"));
                openExcursion.putExtras(myExtras);
                    v.getContext().startActivity(openExcursion);
            }
        });

    }
}

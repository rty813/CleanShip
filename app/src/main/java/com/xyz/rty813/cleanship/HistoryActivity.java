package com.xyz.rty813.cleanship;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;
import com.yanzhenjie.recyclerview.swipe.touch.OnItemMoveListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;

public class HistoryActivity extends AppCompatActivity {
    private TextView tv;
    private ArrayList<Map<String, String>> list;
    private SwipeRecyclerViewAdapter adapter;
    private SwipeMenuRecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        list = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        SQLiteDatabase database = MainActivity.dbHelper.getReadableDatabase();
        Cursor cursor = database.query(MainActivity.dbHelper.TABLE_NAME, null, null, null ,null, null, null);
        if (cursor.getCount() > 0){
            cursor.moveToLast();
            do{
                Map<String, String> map = new HashMap();
                String route = cursor.getString(cursor.getColumnIndex("ROUTE"));
                String time = cursor.getString(cursor.getColumnIndex("TIME"));
                String id = cursor.getString(cursor.getColumnIndex("ID"));
                route = "(" + route.substring(0, route.length() - 2) + ")";
                route = route.replace(";", ") -> \n(");
                map.put("route", route);
                map.put("time", time);
                map.put("id", id);
                list.add(map);
            } while(cursor.moveToPrevious());
        }
        database.close();
        adapter = new SwipeRecyclerViewAdapter(list);
        adapter.notifyDataSetChanged();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setItemViewSwipeEnabled(true);
        recyclerView.setOnItemMoveListener(new OnItemMoveListener() {
            @Override
            public boolean onItemMove(RecyclerView.ViewHolder srcHolder, RecyclerView.ViewHolder targetHolder) {
                return false;
            }

            @Override
            public void onItemDismiss(RecyclerView.ViewHolder srcHolder) {
                int pos = srcHolder.getAdapterPosition();
                String id = list.get(pos).get("id");
                list.remove(pos);
                adapter.notifyItemRemoved(pos);
                SQLiteDatabase database = MainActivity.dbHelper.getWritableDatabase();
                database.beginTransaction();
                database.delete(MainActivity.dbHelper.TABLE_NAME, "ID=?", new String[]{id});
                database.setTransactionSuccessful();
                database.close();
            }
        });


    }
}

package com.xyz.rty813.cleanship;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.yanzhenjie.recyclerview.swipe.SwipeItemClickListener;
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
    private boolean isCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        list = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        SQLiteDatabase database = MainActivity.dbHelper.getReadableDatabase();
        Cursor cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, null, null ,null, null, null);
        if (cursor.getCount() > 0){
            cursor.moveToLast();
            do{
                Map<String, String> map = new HashMap();
                String route = cursor.getString(cursor.getColumnIndex("ROUTE"));
                String time = cursor.getString(cursor.getColumnIndex("TIME"));
                String id = cursor.getString(cursor.getColumnIndex("ID"));
                String address = cursor.getString(cursor.getColumnIndex("ADDRESS"));
                route = "(" + route.substring(0, route.length() - 2) + ")";
                route = route.replace(";", ") -> \n(");
                map.put("detail", time + "\n" + route);
                map.put("title", address);
                map.put("id", id);
                list.add(map);
            } while(cursor.moveToPrevious());
        }
        database.close();
        adapter = new SwipeRecyclerViewAdapter(list);
        adapter.notifyDataSetChanged();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemViewSwipeEnabled(true);
        recyclerView.setOnItemMoveListener(new OnItemMoveListener() {
            @Override
            public boolean onItemMove(RecyclerView.ViewHolder srcHolder, RecyclerView.ViewHolder targetHolder) {
                return false;
            }

            @Override
            public void onItemDismiss(RecyclerView.ViewHolder srcHolder) {
                final int pos = srcHolder.getAdapterPosition();
                final Map<String, String> map = list.get(pos);
                String id = map.get("id");
                list.remove(pos);
                adapter.notifyItemRemoved(pos);
                adapter.notifyItemRangeChanged(pos, list.size() - pos);

                CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorlayout);
                isCancel = false;
                Snackbar.make(coordinatorLayout, "删除记录", Snackbar.LENGTH_LONG)
                        .setAction("撤销", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                isCancel = true;
                                list.add(pos, map);
                                adapter.notifyItemInserted(pos);
                                adapter.notifyItemRangeChanged(pos + 1, list.size() - pos);
                            }
                        })
                        .addCallback(new MyCallBack(id))
                        .show();
            }
        });
        recyclerView.setSwipeItemClickListener(new SwipeItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                Intent intent = new Intent();
                intent.putExtra("id", list.get(position).get("id"));
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        recyclerView.setAdapter(adapter);


    }

    @Override
    protected void onResume() {
        super.onResume();
        MiStatInterface.recordPageStart(this, "历史页");
    }

    @Override
    protected void onPause() {
        super.onPause();
        MiStatInterface.recordPageEnd();
    }

    private class MyCallBack extends Snackbar.Callback{
        private String id;
        public MyCallBack(String id){
            super();
            this.id = id;
        }
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            if (isCancel){
                return;
            }
            SQLiteDatabase database = MainActivity.dbHelper.getWritableDatabase();
            database.delete(SQLiteDBHelper.TABLE_NAME, "ID=?", new String[]{id});
            database.close();
            super.onDismissed(transientBottomBar, event);
        }
    }
}

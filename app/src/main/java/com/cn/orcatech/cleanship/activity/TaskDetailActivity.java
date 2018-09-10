package com.cn.orcatech.cleanship.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.cn.orcatech.cleanship.R;

import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        Map<String, String> map = (Map<String, String>) getIntent().getSerializableExtra("task");
        ((TextView) findViewById(R.id.tv_detail)).setText(map.get("date") + map.get("start_time"));
    }
}

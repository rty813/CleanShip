package com.cn.orcatech.cleanship.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.cn.orcatech.cleanship.R;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    private MapView mMapView;
    private AMap aMap;
    private ArrayList<LatLng> traceList;
    private SmoothMoveMarker smoothMoveMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        Map<String, String> map = (Map<String, String>) getIntent().getSerializableExtra("task");
        traceList = new ArrayList<>();

        ((TextView) findViewById(R.id.tv_date)).setText(map.get("date"));
        ((TextView) findViewById(R.id.tv_length)).setText(map.get("length"));
        ((TextView) findViewById(R.id.tv_ise)).setText("ISE：" + map.get("ise"));
        ((TextView) findViewById(R.id.tv_variance)).setText("方差：" + map.get("variance"));
        ((TextView) findViewById(R.id.tv_timeCost)).setText("耗时：" + map.get("timeCost") + "秒");

        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        aMap.getUiSettings().setCompassEnabled(false);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
        smoothMoveMarker = new SmoothMoveMarker(aMap);
        smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromView(View.inflate(this, R.layout.ship, null)));
        loadRoute(getIntent().getIntExtra("id", 1),
                getIntent().getIntExtra("ship_id", 5),
                map.get("start_time"), map.get("end_time"), map.get("timeCost"));
    }

    private void loadRoute(int id, int ship_id, String start_time, String end_time, final String timeCost) {
        StringRequest request = new StringRequest("http://orca-tech.cn/app/history_select.php", RequestMethod.POST);
        request.add("ship_id", ship_id).add("id", id).add("start_time", start_time)
                .add("end_time", end_time).add("content", "lat,lng");
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            @Override
            public void onSucceed(int what, Response<String> response) {
                super.onSucceed(what, response);
                new MyAsyncTask(TaskDetailActivity.this).execute(response.get(), timeCost);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    private static class MyAsyncTask extends AsyncTask<String, Double, Void> {
        private WeakReference<TaskDetailActivity> activity;
        private LatLng prePoint;
        private List<LatLng> points;

        MyAsyncTask(TaskDetailActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                JSONArray array = new JSONArray(strings[0]);
                for (int i = array.length() - 1; i >= 0; i--) {
                    JSONObject object = array.getJSONObject(i);
                    publishProgress((double) (array.length() - i - 1), object.getDouble("lat"), object.getDouble("lng"));
                    Thread.sleep(100);
                }
            } catch (JSONException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            super.onProgressUpdate(values);
            TaskDetailActivity activity = this.activity.get();
            if (values[0] == 0) {
                LatLngBounds bounds = new LatLngBounds(new LatLng(values[1], values[2]), new LatLng(values[1], values[2]));
                activity.aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30));
                prePoint = new LatLng(values[1], values[2]);
            }
            points = new ArrayList<>();
            points.add(new LatLng(prePoint.latitude, prePoint.longitude));
            prePoint = new LatLng(values[1], values[2]);
            points.add(new LatLng(prePoint.latitude, prePoint.longitude));
            activity.smoothMoveMarker.setPoints(points);
            activity.smoothMoveMarker.setTotalDuration(1);
            activity.smoothMoveMarker.startSmoothMove();
            activity.smoothMoveMarker.getMarker().setInfoWindowEnable(false);
            activity.smoothMoveMarker.getMarker().setFlat(true);
            activity.smoothMoveMarker.getMarker().setAnchor(0.5f, 0.5f);
            activity.aMap.addPolyline(new PolylineOptions()
                    .add(points.get(0), points.get(1)).width(7).color(0xFFFF0000));
        }
    }
}

package com.xyz.rty813.cleanship;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapOptions;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AMap.OnMapClickListener, AMap.OnMarkerClickListener, View.OnClickListener {
    private MapView mMapView;
    private AMap aMap;
    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        if (aMap == null) {
            aMap = mMapView.getMap();
        }

        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnMapClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_detail).setOnClickListener(this);
        findViewById(R.id.btn_history).setOnClickListener(this);
        findViewById(R.id.btn_save).setOnClickListener(this);
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
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapClick(LatLng latLng) {
//        System.out.println(latLng.toString());
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        markerOptions.title(String.valueOf(markers.size() + 1));
        markerOptions.snippet("经度：" + latLng.latitude + "\n纬度：" + latLng.longitude);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));

        Marker marker = aMap.addMarker(markerOptions);
        markers.add(marker);
        if (markers.size() > 1) {
            LatLng latLng1 = markers.get(markers.size() - 2).getPosition();
            LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
            polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(6).color(Color.RED)));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
//        Toast.makeText(this,"H", Toast.LENGTH_SHORT).show();
        if (marker.isInfoWindowShown()) {
            marker.hideInfoWindow();
        } else {
            marker.showInfoWindow();
        }
        if ((markers.size() > 1) && (marker.getTitle().equals("1")) && (!markers.get(markers.size() - 1).getTitle().equals("1"))) {
            marker.hideInfoWindow();
            LatLng latLng = markers.get(markers.size() - 1).getPosition();
            MarkerOptions markerOptions = new MarkerOptions().position(marker.getPosition());
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
            markers.add(aMap.addMarker(markerOptions));
            polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng, marker.getPosition()).width(6).color(Color.RED)));
            Toast.makeText(this, "完成闭合回路！", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                if (markers.size() > 0) {
                    Marker marker = markers.get(markers.size() - 1);
                    marker.hideInfoWindow();
                    markers.get(markers.size() - 1).destroy();
                    markers.remove(markers.size() - 1);
                    if (polylines.size() > 0) {
                        polylines.get(polylines.size() - 1).remove();
                        polylines.remove(polylines.size() - 1);
                    }
                }
                break;
            case R.id.btn_clear:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("提示");
                builder.setMessage("是否要清除所有标记？");
                builder.setNegativeButton("否", null);
                builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        aMap.clear();
                        markers.removeAll(markers);
                        polylines.removeAll(polylines);
                    }
                });
                builder.show();
                break;
            case R.id.btn_detail:
//                StringBuilder stringBuilder = new StringBuilder();
//                for (Marker marker : markers){
//                    stringBuilder.append(String.valueOf(marker.getPosition().latitude) + "," + String.valueOf(marker.getPosition().longitude) + ";");
//                }
                break;

            case R.id.btn_start:
                Toast.makeText(this, "Go", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_history:
                startActivity(new Intent(this, HistoryActivity.class));
                break;
            case R.id.btn_save:
                final EditText editText = new EditText(this);
                editText.setHint("请输入路线名称");
                builder = new AlertDialog.Builder(this);
                builder.setTitle("保存");
                builder.setView(editText, 50,20,50,20);
                builder.setNegativeButton("取消", null);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String routeName = editText.getText().toString();
                        if (routeName.equals("")){
                            Toast.makeText(MainActivity.this, "名称不能为空！", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            StringBuilder stringBuilder = new StringBuilder();
                            for (Marker marker : markers){
                                stringBuilder.append(String.valueOf(marker.getPosition().latitude) + "," + String.valueOf(marker.getPosition().longitude) + ";");
                            }
                            saveRoute(stringBuilder.toString(), "HISTORY");
                        }
                    }
                });
                builder.show();
                break;
        }
    }

    private void saveRoute(String routeStr, String dbname){

    }
}


package com.xyz.rty813.cleanship;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.dd.morphingbutton.MorphingButton;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.xyz.rty813.cleanship.util.SerialPortTool;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AMap.OnMapClickListener, AMap.OnMarkerClickListener, View.OnClickListener, GeocodeSearch.OnGeocodeSearchListener {
    private MapView mMapView;
    private AMap aMap;
    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;
    public static SQLiteDBHelper dbHelper;
    private SerialPortTool serialPort;
    private MorphingButton btn_start;
    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int HASGO = 2;
    private int state = UNREADY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new SQLiteDBHelper(this);
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        serialPort = new SerialPortTool(this);
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        initSerialPort(115200);
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnMapClickListener(this);
        btn_start = findViewById(R.id.btn_start);
        morph(state, 0);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_detail).setOnClickListener(this);
        findViewById(R.id.btn_history).setOnClickListener(this);

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
                startActivity(new Intent(this, SerialPortActivity.class));
                break;

            case R.id.btn_start:
                if (state != READY){
                    morph(READY, 300);
                    state = READY;
                }
                else {
                    morph(UNREADY, 300);
                    state = UNREADY;
                }
                if (markers.size() > 0){
                    LatLng latLng = markers.get(0).getPosition();
                    GeocodeSearch geocodeSearch = new GeocodeSearch(this);
                    geocodeSearch.setOnGeocodeSearchListener(this);
                    RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 1000, GeocodeSearch.AMAP);
                    geocodeSearch.getFromLocationAsyn(query);
                }
                else{
                    loadRoute(null);
                }
                break;
            case R.id.btn_history:
                startActivityForResult(new Intent(this, HistoryActivity.class), 200);
                break;
        }
    }

    private void morph(int state, int duration){
        MorphingButton.Params params = null;
        switch (state){
            case UNREADY:
                params = MorphingButton.Params.create()
                        .duration(duration)
                        .cornerRadius(dimen(R.dimen.mb_height_56))
                        .width(dimen(R.dimen.mb_height_56))
                        .height(dimen(R.dimen.mb_height_56))
                        .color(color(R.color.mb_blue))
                        .colorPressed(color(R.color.mb_blue))
                        .icon(R.drawable.connect2);
                break;
            case READY:
                params = MorphingButton.Params.create()
                        .duration(duration)
                        .cornerRadius(dimen(R.dimen.mb_corner_radius_8))
                        .width(dimen(R.dimen.mb_width_100))
                        .height(dimen(R.dimen.mb_height_56))
                        .color(color(R.color.mb_green))
                        .colorPressed(color(R.color.mb_green))
                        .text("Go");
                break;
            case HASGO:
                params = MorphingButton.Params.create()
                        .duration(duration)
                        .cornerRadius(dimen(R.dimen.mb_corner_radius_8))
                        .width(dimen(R.dimen.mb_width_100))
                        .height(dimen(R.dimen.mb_height_56))
                        .color(color(R.color.mb_red))
                        .colorPressed(color(R.color.mb_red))
                        .text("Stop");
                break;
        }
        btn_start.morph(params);
    }

    private int dimen(@DimenRes int resId) {
        return (int) getResources().getDimension(resId);
    }

    public int color(@ColorRes int resId) {

        return getResources().getColor(resId);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == 200){
                if (data.getStringExtra("id") != null){
                    loadRoute(data.getStringExtra("id"));
                }
            }
        }
    }

    private void initSerialPort(int baudRate){
        List<UsbSerialDriver> list = serialPort.searchSerialPort();
        if (list.isEmpty()){
            Toast.makeText(this, "未连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            serialPort.initDevice(list.get(0));
            serialPort.openDevice(baudRate);
            state = READY;
            morph(READY, 500);
        } catch (IOException e) {
            Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveRoute(String time, String route, String address){
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("TIME", time);
        cv.put("ROUTE", route);
        cv.put("ADDRESS", address);
        database.insert(SQLiteDBHelper.TABLE_NAME, null, cv);
        database.close();
    }

    private void loadRoute(@Nullable String id){
        SQLiteDatabase database = MainActivity.dbHelper.getReadableDatabase();
        aMap.clear();
        markers.removeAll(markers);
        polylines.removeAll(polylines);
        Cursor cursor;
        if (id != null){
            cursor = database.query(MainActivity.dbHelper.TABLE_NAME, null, "ID=?", new String[]{id} ,null, null, null);
            cursor.moveToFirst();
        }
        else{
            cursor = database.query(MainActivity.dbHelper.TABLE_NAME, null, null, null ,null, null, null);
            cursor.moveToLast();
        }
        if (cursor.getCount() > 0){
            String route = cursor.getString(cursor.getColumnIndex("ROUTE"));
            String[] markers_str = route.split(";");
            for (int i = 0; i < markers_str.length; i++){
                String[] location = markers_str[i].split(",");
                LatLng latLng = new LatLng(Float.parseFloat(location[0]), Float.parseFloat(location[1]));
                if (i == 0){
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(latLng, 18, 0, 0));
                    aMap.animateCamera(cameraUpdate);
                }
                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
                markerOptions.title(String.valueOf(markers.size() + 1));
                markerOptions.snippet("纬度：" + latLng.latitude + "\n经度：" + latLng.longitude);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                markers.add(aMap.addMarker(markerOptions));
                if (markers.size() > 1) {
                    LatLng latLng1 = markers.get(markers.size() - 2).getPosition();
                    LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(6).color(Color.RED)));
                }
            }


        }
        database.close();
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        String pos = null;
        if (i == 1000){
            RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
            pos = address.getProvince();
            if (!address.getProvince().equals(address.getCity())){
                pos += " " + address.getCity();
            }
            pos +=  " " + address.getDistrict();
            if (address.getPois().size() != 0){
                pos = pos + " " + address.getPois().get(0);
            }
            System.out.println(pos);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Marker marker : markers){
            stringBuilder.append(String.valueOf(marker.getPosition().latitude) + "," + String.valueOf(marker.getPosition().longitude) + ";");
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        saveRoute(dateFormat.format(new Date(System.currentTimeMillis())), stringBuilder.toString(), pos);
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }
}


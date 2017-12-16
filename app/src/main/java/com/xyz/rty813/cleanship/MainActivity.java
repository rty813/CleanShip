package com.xyz.rty813.cleanship;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.PopupWindow;
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
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.kcode.lib.UpdateWrapper;
import com.kcode.lib.bean.VersionModel;
import com.kcode.lib.net.CheckUpdateTask;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
import com.xyz.rty813.cleanship.util.CoordinateConverter;
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.xyz.rty813.cleanship.util.SerialPortTool;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.dinus.com.loadingdrawable.render.LoadingRenderer;

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
    private static final int GONE = 2;
    public static int state = UNREADY;
    private String pos = null;
    private float alpha = 1.0f;
    private static final String MY_APPID = "2882303761517676503";
    private static final String MY_APP_KEY = "5131767662503";
    private static final String CHANNEL = "SELF";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("onCreate");
        MiStatInterface.initialize(this, MY_APPID, MY_APP_KEY, CHANNEL);
        MiStatInterface.setUploadPolicy(MiStatInterface.UPLOAD_POLICY_REALTIME, 0);
        URLStatsRecorder.enableAutoRecord();
        checkUpdate();
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        finish();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                })
                .check();
        dbHelper = new SQLiteDBHelper(this);
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        serialPort = new SerialPortTool(this);
        btn_start = findViewById(R.id.btn_start);
        morph(state, 0);
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnMapClickListener(this);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_detail).setOnClickListener(this);
        findViewById(R.id.btn_history).setOnClickListener(this);

    }

    private void checkUpdate() {
        System.out.println("check update");
        UpdateWrapper updateWrapper = new UpdateWrapper.Builder(getApplicationContext())
                //set interval Time
                .setTime(1000)
                //set notification icon
                .setNotificationIcon(R.mipmap.ic_launcher)
                //set update file url
                .setUrl("http://rty813.xyz/cleanship.json")
                //set showToast. default is true
                .setIsShowToast(false)
                //add callback ,return new version info
                .setCallback(new CheckUpdateTask.Callback() {
                    @Override
                    public void callBack(VersionModel versionModel) {
                        Log.d("Update","new version :" + versionModel.getVersionCode() + ";version info" + versionModel.getVersionName());
                    }
                })
                .build();
        updateWrapper.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MiStatInterface.recordPageEnd();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("onDestory");
        state = UNREADY;
        serialPort.unregisterReceiver();
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MiStatInterface.recordPageStart(this, "主界面");
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
        markerOptions.snippet("纬度：" + latLng.latitude + "\n经度：" + latLng.longitude);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));

        Marker marker = aMap.addMarker(markerOptions);
        System.out.println(latLng.toString());
        markers.add(marker);
        if (markers.size() > 1) {
            LatLng latLng1 = markers.get(markers.size() - 2).getPosition();
            LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
            polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(6).color(Color.RED)));
        }
        else {
            GeocodeSearch geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
            RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 1000, GeocodeSearch.AMAP);
            geocodeSearch.getFromLocationAsyn(query);
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
                if (markers.size() > 0){
                    final Handler mHandler = new Handler(){
                        @Override
                        public void handleMessage(Message msg) {
                            switch (msg.what){
                                case 1:
                                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                                    lp.alpha = (float) msg.obj;
                                    getWindow().setAttributes(lp);
                                    break;
                                case 2:
                                    ((PopupWindow)msg.obj).showAtLocation(mMapView, Gravity.CENTER, 0, 0);
                            }
                        }
                    };
                    final View popupview = View.inflate(MainActivity.this,R.layout.popupwindow_detail,null);
                    final PopupWindow popupWindow = new PopupWindow(popupview, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
                    CollapsingToolbarLayout collapsingToolbarLayout = popupview.findViewById(R.id.collapsingtoolbarlayout);
                    aMap.getMapScreenShot(new AMap.OnMapScreenShotListener() {
                        @Override
                        public void onMapScreenShot(Bitmap bitmap) {
                            popupview.findViewById(R.id.iv_map).setBackground(new BitmapDrawable(bitmap));
                            alpha = 1.0f;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(alpha > 0.5f){
                                        try{
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Message msg = mHandler.obtainMessage();
                                        msg.what = 1;
                                        alpha -= 0.01f;
                                        msg.obj = alpha;
                                        mHandler.sendMessage(msg);
                                    }
                                    Message msg = mHandler.obtainMessage();
                                    msg.what = 2;
                                    msg.obj = popupWindow;
                                    mHandler.sendMessage(msg);
                                }
                            }).start();
                        }
                    });

                    collapsingToolbarLayout.setTitle(pos);
                    collapsingToolbarLayout.setExpandedTitleColor(Color.DKGRAY);
                    collapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
                    RecyclerView recyclerView = popupview.findViewById(R.id.recyclerView_detail);
                    ArrayList<String> detailList = new ArrayList<>();
                    for (int i = 0; i < markers.size(); i++){
                        LatLonPoint point = CoordinateConverter.toWGS84Point(markers.get(i).getPosition().latitude, markers.get(i).getPosition().longitude);
                        detailList.add(String.format(Locale.CHINA, "%d：纬度=%.6f\t 经度=%.6f", i+1, point.getLatitude(), point.getLongitude()));
                    }
                    detailList.add("");
                    detailList.add("");
                    DetailRecyclerViewAdapter adapter = new DetailRecyclerViewAdapter(detailList);
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                    linearLayoutManager.setSmoothScrollbarEnabled(true);
                    linearLayoutManager.setAutoMeasureEnabled(true);
                    recyclerView.setLayoutManager(linearLayoutManager);
                    recyclerView.setHasFixedSize(true);
                    recyclerView.setAdapter(adapter);

                    popupWindow.setOutsideTouchable(true);
                    popupWindow.setFocusable(true);
                    popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
                    popupWindow.setAnimationStyle(R.style.dismiss_anim);
                    popupWindow.update();
                    popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(alpha < 1f){
                                        try {
                                            Thread.sleep(3);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Message msg = mHandler.obtainMessage();
                                        msg.what = 1;
                                        alpha += 0.01f;
                                        msg.obj = alpha;
                                        mHandler.sendMessage(msg);
                                    }
                                }
                            }).start();

                        }
                    });
                }
                break;

            case R.id.btn_start:
                switch (state){
                    case READY:
                        if (markers.size() > 0){
                            StringBuilder stringBuilder = new StringBuilder();
                            for (Marker marker : markers){
                                LatLonPoint point = CoordinateConverter.toWGS84Point(marker.getPosition().latitude, marker.getPosition().longitude);
                                stringBuilder.append(String.format(Locale.getDefault(), "%.6f,%.6f;", point.getLatitude(), point.getLongitude()));
                            }
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                            saveRoute(dateFormat.format(new Date(System.currentTimeMillis())), stringBuilder.toString(), pos);
                            final Handler mHandler = new Handler(){
                                @Override
                                public void handleMessage(Message msg) {
                                    AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
                                    animation.setDuration(300);
                                    animation.setInterpolator(new AccelerateDecelerateInterpolator());
                                    animation.setAnimationListener(new Animation.AnimationListener() {
                                        @Override
                                        public void onAnimationStart(Animation animation) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animation animation) {
                                            findViewById(R.id.loadingview).setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {

                                        }
                                    });
                                    findViewById(R.id.loadingview).startAnimation(animation);
                                    switch (msg.what){
                                        case 1:
                                            Toast.makeText(MainActivity.this, "发送失败！", Toast.LENGTH_SHORT).show();
                                            state = UNREADY;
                                            morph(state,200);
                                            serialPort.closeDevice();
                                            break;
                                        case 2:
                                            Toast.makeText(MainActivity.this, "已发送", Toast.LENGTH_SHORT).show();
                                            morph(GONE, 300);
                                            break;
                                    }
                                }
                            };
                            findViewById(R.id.loadingview).setVisibility(View.VISIBLE);
                            AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
                            animation.setDuration(300);
                            animation.setInterpolator(new AccelerateDecelerateInterpolator());
                            findViewById(R.id.loadingview).startAnimation(animation);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    Message msg = mHandler.obtainMessage();
                                    for (Marker marker : markers){
                                        LatLonPoint point = CoordinateConverter.toWGS84Point(marker.getPosition().latitude, marker.getPosition().longitude);
                                        double latitude = point.getLatitude() * 100;
                                        double longitude = point.getLongitude() * 100;
                                        System.out.println(stringBuilder.toString());
                                        try {
                                            serialPort.writeData(String.format(Locale.getDefault(), "$GNGGA,0,%.5f,0,%.5f,#",latitude, longitude));
                                            Thread.sleep(200);
                                        } catch (Exception e){
                                            e.printStackTrace();
                                            msg.what = 1;
                                            mHandler.sendMessage(msg);
                                            return;
                                        }
                                    }
                                    try {
                                        serialPort.writeData("$CALC#");
                                        Thread.sleep(200);
                                        serialPort.writeData("$XJ#");
                                        Thread.sleep(200);
                                        msg.what = 2;
                                        mHandler.sendMessage(msg);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        msg.what = 1;
                                        mHandler.sendMessage(msg);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                        else{
                            loadRoute(null);
                        }
                        break;
                    case UNREADY:
                        initSerialPort(115200);
                        break;
                    case GONE:
                        try {
                            serialPort.writeData("$STOP#\n");
                            Toast.makeText(MainActivity.this, "已结束", Toast.LENGTH_SHORT).show();
                            morph(READY, 300);
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "发送失败！", Toast.LENGTH_SHORT).show();
                            state = UNREADY;
                            morph(state,200);
                            serialPort.closeDevice();
                        }
                        aMap.clear();
                        markers.removeAll(markers);
                        polylines.removeAll(polylines);
                }
                break;
            case R.id.btn_history:
                startActivityForResult(new Intent(this, HistoryActivity.class), 200);
                break;
        }
    }

    public void morph(int state, int duration){
        MainActivity.state = state;
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
                        .icon(R.drawable.connect);
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
            case GONE:
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
        System.out.println("init serial port");
        List<UsbSerialDriver> list = serialPort.searchSerialPort();
        if (list.isEmpty()){
            Toast.makeText(this, "未连接设备", Toast.LENGTH_SHORT).show();
            morph(UNREADY, 0);
        }else{
            serialPort.initDevice(list.get(0), baudRate);
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
                LatLonPoint point = CoordinateConverter.toGCJ02Point(Float.parseFloat(location[0]), Float.parseFloat(location[1]));
                LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
                if (i == 0){
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(latLng, 18, 0, 0));
                    aMap.moveCamera(cameraUpdate);
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
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }


}


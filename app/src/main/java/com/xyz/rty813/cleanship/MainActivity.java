package com.xyz.rty813.cleanship;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
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
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
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
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.xyz.rty813.cleanship.util.SerialPortTool;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.dinus.com.loadingdrawable.LoadingView;

public class MainActivity extends AppCompatActivity implements AMap.OnMapClickListener, AMap.OnMarkerClickListener, View.OnClickListener, GeocodeSearch.OnGeocodeSearchListener, SerialPortTool.onConnectedListener {
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
    private static final int NAV = 3;
    public static int state = UNREADY;
    private String pos = null;
    private float alpha = 1.0f;
    private static final String MY_APPID = "2882303761517676503";
    private static final String MY_APP_KEY = "5131767662503";
    private static final String CHANNEL = "SELF";
    private MyReceiver receiver;
    private ArrayList<LatLng> shipPointList;
    private ArrayList<LatLng> aimPointList;
    private SmoothMoveMarker smoothMoveMarker;
    private LoadingView loadingView;
    private TextView tvAimAngle;
    private TextView tvGyroAngle;
    private TextView tvCurrGas;
    private TextView tvCurrLat;
    private TextView tvCurrLng;
    private TextView tvRawData;
    private TextView tvGpsNum;
    private ImageView ivPointerAim;
    private ImageView ivPointerGyro;
    private double lastAimAngle;
    private double lastGyroAngle;
    public MyHandler mHandler = null;
    private Thread myThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("onCreate");
        MiStatInterface.initialize(this, MY_APPID, MY_APP_KEY, CHANNEL);
        MiStatInterface.setUploadPolicy(MiStatInterface.UPLOAD_POLICY_REALTIME, 0);
        MiStatInterface.enableExceptionCatcher(true);
        URLStatsRecorder.enableAutoRecord();
        dbHelper = new SQLiteDBHelper(this);
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        MyLocationStyle myLocationStyle = new MyLocationStyle();
                        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
                        aMap.setMyLocationStyle(myLocationStyle);
                        aMap.setMyLocationEnabled(true);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "请赋予定位权限", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                })
                .check();
        serialPort = new SerialPortTool(this);
        serialPort.setListener(this);
        myThread = new Thread(new QueryThread());
        myThread.start();
        btn_start = findViewById(R.id.btn_start);
        morph(state, 0);
        mHandler = new MyHandler();
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnMapClickListener(this);
        initSmoothMove();
        aimPointList = new ArrayList<>();
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_detail).setOnClickListener(this);
        findViewById(R.id.btn_history).setOnClickListener(this);
        findViewById(R.id.btn_plane).setOnClickListener(this);
        findViewById(R.id.btn_satellite).setOnClickListener(this);
        tvAimAngle = findViewById(R.id.tv_aim_angle);
        tvCurrGas = findViewById(R.id.tv_curr_gas);
        tvGyroAngle = findViewById(R.id.tv_gyro_angle);
        tvCurrLat = findViewById(R.id.tv_curr_lat);
        tvCurrLng = findViewById(R.id.tv_curr_lng);
        tvRawData = findViewById(R.id.tv_raw_data);
        tvGpsNum = findViewById(R.id.tv_gps_num);
        ivPointerAim = findViewById(R.id.ivPointerAim);
        ivPointerGyro = findViewById(R.id.ivPointerGyro);
        loadingView = findViewById(R.id.loadingview);
        ivPointerAim.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams params = ivPointerAim.getLayoutParams();
                params.height = params.height * 2;
                ivPointerAim.setLayoutParams(params);
            }
        });
        checkUpdate();
        ((SeekBar)findViewById(R.id.seekbar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setAimAngle(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onConnected() {
        Toast.makeText(this, "已连接", Toast.LENGTH_SHORT).show();
        morph(READY, 200);
        synchronized (serialPort){
            serialPort.notify();
        }
    }

    private class QueryThread implements Runnable {
        @Override
        public void run() {
            synchronized (serialPort){
                int type = 0;
                while (true) {
                    try {
                        if (state == UNREADY){
                            serialPort.wait();
                        }
                        Thread.sleep(100);
                        serialPort.writeData(String.format(Locale.getDefault(), "$QUERY,%d#", type), 100);
                        String data = serialPort.readData();
                        if (data != null && !data.equals("")) {
                            String[] strings = data.split(";");
                            Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                            if (strings.length == 2){
                                intent.putExtra("rawData", "|" + strings[0] + "|" + strings[1] + "|");
                            }
                            else{
                                intent.putExtra("rawData", data);
                            }
                            if (strings.length == 2 && Integer.parseInt(strings[0]) == type) {
                                intent.putExtra("type", type);
                                intent.putExtra("data", strings[1]);
                                type = (type + 1) % 6;
                            }
                            sendBroadcast(intent);
                        }
                        Thread.sleep(100);
                    } catch (NumberFormatException | InterruptedException e) {
                        mHandler.sendEmptyMessage(5);
                        e.printStackTrace();
                    } catch (IOException e){
                        e.printStackTrace();
                        mHandler.sendEmptyMessage(3);
                    }
                }
            }
        }
    }

    public class MyHandler extends Handler{
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
                    loadingView.setVisibility(View.GONE);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            switch (msg.what){
                case 1:
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.alpha = (float) msg.obj;
                    getWindow().setAttributes(lp);
                    break;
                case 2:
                    ((PopupWindow)msg.obj).showAtLocation(mMapView, Gravity.CENTER, 0, 0);
                    break;
                case 3:
                    if (loadingView.getVisibility() == View.VISIBLE){
                        loadingView.startAnimation(animation);
                    }
                    morph(UNREADY, 200);
                    Toast.makeText(MainActivity.this, "连接中断！", Toast.LENGTH_SHORT).show();
                    serialPort.closeDevice();
                    break;
                case 4:
                    if (loadingView.getVisibility() == View.VISIBLE){
                        loadingView.startAnimation(animation);
                    }
                    synchronized (serialPort){
                        serialPort.notify();
                        morph(NAV, 300);
                    }
                    break;
                case 5:
                    Toast.makeText(MainActivity.this, "非法数据", Toast.LENGTH_SHORT).show();
                    break;
                case 6:
                    if (loadingView.getVisibility() == View.VISIBLE){
                        loadingView.startAnimation(animation);
                    }
                    break;
                case 7:
                    btn_start.setEnabled(true);
                    break;
                case 8:
                    synchronized (serialPort) {
                        if (loadingView.getVisibility() == View.VISIBLE){
                            loadingView.startAnimation(animation);
                        }
                        morph((Integer) msg.obj, 300);
                        serialPort.notify();
                    }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void checkUpdate() {
        System.out.println("check update");
        UpdateWrapper updateWrapper = new UpdateWrapper.Builder(getApplicationContext())
                .setTime(1000)
                .setNotificationIcon(R.mipmap.ic_launcher)
                .setUrl("http://rty813.xyz/cleanship.json")
                .setIsShowToast(false)
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
        unregisterReceiver(receiver);
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
        receiver = new MyReceiver();
        registerReceiver(receiver, new IntentFilter(MyReceiver.ACTION_DATA_RECEIVED));
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
        markerOptions.snippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
        markerOptions.anchor(0.5f, 0.5f);

        Marker marker = aMap.addMarker(markerOptions);
        System.out.println(latLng.toString());
        markers.add(marker);
        if (markers.size() > 1) {
            LatLng latLng1 = markers.get(markers.size() - 2).getPosition();
            LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
            polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(10).color(Color.RED)));
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
            markerOptions.anchor(0.5f, 0.5f);
            markers.add(aMap.addMarker(markerOptions));
            polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng, marker.getPosition()).width(10).color(Color.RED)));
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
                        resetMap();
                    }
                });
                builder.show();
                break;

            case R.id.btn_detail:
                if (markers.size() > 0){
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

                        @Override
                        public void onMapScreenShot(Bitmap bitmap, int i) {

                        }
                    });

                    collapsingToolbarLayout.setTitle(pos);
                    collapsingToolbarLayout.setExpandedTitleColor(Color.DKGRAY);
                    collapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
                    RecyclerView recyclerView = popupview.findViewById(R.id.recyclerView_detail);
                    ArrayList<String> detailList = new ArrayList<>();
                    for (int i = 0; i < markers.size(); i++){
                        detailList.add(String.format(Locale.CHINA, "%d：纬度=%.6f\t 经度=%.6f", i+1, markers.get(i).getPosition().latitude, markers.get(i).getPosition().longitude));
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
                                stringBuilder.append(String.format(Locale.getDefault(), "%.6f,%.6f;", marker.getPosition().latitude, marker.getPosition().longitude));
                            }
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
                            saveRoute(dateFormat.format(new Date(System.currentTimeMillis())), stringBuilder.toString(), pos);
                            showLoadingView();
                            state = UNREADY;
                            btn_start.setEnabled(false);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        for (Marker marker : markers){
                                            double latitude = marker.getPosition().latitude;
                                            double longitude = marker.getPosition().longitude;
                                            serialPort.writeData(String.format(Locale.getDefault(), "$GNGGA,%.6f,%.6f#",latitude, longitude), 100);
                                        }
                                        mHandler.sendEmptyMessage(4);
                                    } catch (InterruptedException | IOException e) {
                                        e.printStackTrace();
                                        mHandler.sendEmptyMessage(3);
                                    } finally {
                                        mHandler.sendEmptyMessage(7);
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
                    case NAV:
                            state = UNREADY;
                            showLoadingView();
                            btn_start.setEnabled(false);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        do {
                                            Thread.sleep(200);
                                            serialPort.writeData("$NAV#", 100);
                                            String data = serialPort.readData();
                                            if (data != null && data.contains("$NAV#")){
                                                Message msg = mHandler.obtainMessage();
                                                msg.what = 8;
                                                msg.obj = GONE;
                                                mHandler.sendMessage(msg);
                                                break;
                                            }
                                            Thread.sleep(200);
                                        } while (true);
                                    } catch (IOException | InterruptedException e) {
                                        e.printStackTrace();
                                        mHandler.sendEmptyMessage(3);
                                    } finally {
                                        mHandler.sendEmptyMessage(7);
                                    }
                                }
                            }).start();
                        break;
                    case GONE:
                        state = UNREADY;
                        showLoadingView();
                        btn_start.setEnabled(false);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    do {
                                        Thread.sleep(200);
                                        serialPort.writeData("$STOP#", 100);
                                        String data = serialPort.readData();
                                        if (data != null && data.contains("$STOP#")){
                                            Message msg = mHandler.obtainMessage();
                                            msg.what = 8;
                                            msg.obj = READY;
                                            mHandler.sendMessage(msg);
                                            break;
                                        }
                                        Thread.sleep(200);
                                    } while (true);
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                    mHandler.sendEmptyMessage(3);
                                } finally {
                                    mHandler.sendEmptyMessage(7);
                                }
                            }
                        }).start();
//                        resetMap();
                        break;
                }
                break;
            case R.id.btn_history:
                startActivityForResult(new Intent(this, HistoryActivity.class), 200);
                break;
            case R.id.btn_plane:
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                break;
            case R.id.btn_satellite:
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                break;
        }
    }

    private void showLoadingView() {
        findViewById(R.id.loadingview).setVisibility(View.VISIBLE);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        findViewById(R.id.loadingview).startAnimation(animation);
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
                        .text("CALC");
                break;
            case NAV:
                params = MorphingButton.Params.create()
                        .duration(duration)
                        .cornerRadius(dimen(R.dimen.mb_corner_radius_8))
                        .width(dimen(R.dimen.mb_width_100))
                        .height(dimen(R.dimen.mb_height_56))
                        .color(color(R.color.mb_green))
                        .colorPressed(color(R.color.mb_green))
                        .text("NAV");
                break;
            case GONE:
                params = MorphingButton.Params.create()
                        .duration(duration)
                        .cornerRadius(dimen(R.dimen.mb_corner_radius_8))
                        .width(dimen(R.dimen.mb_width_100))
                        .height(dimen(R.dimen.mb_height_56))
                        .color(color(R.color.mb_red))
                        .colorPressed(color(R.color.mb_red))
                        .text("STOP");
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
            try {
                serialPort.initDevice(list.get(0), baudRate);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show();
                morph(UNREADY, 0);
            }
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
        resetMap();
        Cursor cursor;
        if (id != null){
            cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, "ID=?", new String[]{id} ,null, null, null);
            cursor.moveToFirst();
        }
        else{
            cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, null, null ,null, null, null);
            cursor.moveToLast();
        }
        if (cursor.getCount() > 0){
            String route = cursor.getString(cursor.getColumnIndex("ROUTE"));
            String[] markers_str = route.split(";");
            for (int i = 0; i < markers_str.length; i++){
                String[] location = markers_str[i].split(",");
                LatLng latLng = new LatLng(Double.parseDouble(location[0]), Double.parseDouble(location[1]));
                if (i == 0){
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(latLng, 18, 0, 0));
                    aMap.animateCamera(cameraUpdate);
                }
                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
                markerOptions.title(String.valueOf(markers.size() + 1));
                markerOptions.snippet("纬度：" + latLng.latitude + "\n经度：" + latLng.longitude);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                markerOptions.anchor(0.5f, 0.5f);
                markers.add(aMap.addMarker(markerOptions));
                if (markers.size() > 1) {
                    LatLng latLng1 = markers.get(markers.size() - 2).getPosition();
                    LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(10).color(Color.RED)));
                }
            }
        }
        cursor.close();
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

    public ArrayList<LatLng> getShipPointList() {
        return shipPointList;
    }

    private void initSmoothMove(){
        shipPointList = new ArrayList<>();
        shipPointList.add(new LatLng(0, 0));
        smoothMoveMarker = new SmoothMoveMarker(aMap);
        smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.ship));

    }

    private void focusShip(){
        LatLngBounds bounds = new LatLngBounds(shipPointList.get(1), shipPointList.get(shipPointList.size() - 1));
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

    public void move(){
        if (shipPointList.size() == 2){
            focusShip();
        }
        else{
            List<LatLng> subList = shipPointList.subList(shipPointList.size() - 2, shipPointList.size());
            smoothMoveMarker.setPoints(subList);
            smoothMoveMarker.setTotalDuration(1);
            smoothMoveMarker.startSmoothMove();
            aMap.addPolyline(new PolylineOptions().add(shipPointList.get(shipPointList.size() - 2),
                    shipPointList.get(shipPointList.size() - 1)).width(5).color(Color.rgb(61, 110, 234)));
        }
    }

    public void setCurrGas(String currGas) {
        this.tvCurrGas.setText(currGas);
    }

    public void setCurrLatlng(String lat, String lng) {
        this.tvCurrLat.setText(lat);
        this.tvCurrLng.setText(lng);
    }

    public void setGpsNum(String gpsNum) {
        this.tvGpsNum.setText(gpsNum);
    }

    public void setRawData(String rawData){
        if (this.tvRawData.getText().toString().split("\n").length > 5){
            this.tvRawData.setText("");
        }
        this.tvRawData.setText(this.tvRawData.getText().toString() + rawData + "\n");
    }

    public void setAimAngle(double aimAngle){
        this.tvAimAngle.setText(String.valueOf(aimAngle));
        RotateAnimation animation = new RotateAnimation((float)lastAimAngle, (float)aimAngle,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(100);
        animation.setFillAfter(true);
        ivPointerAim.startAnimation(animation);
        lastAimAngle = aimAngle;
    }

    public void setGyroAngle(double gyroAngle){
        this.tvGyroAngle.setText(String.valueOf(gyroAngle));
        RotateAnimation animation = new RotateAnimation((float)lastGyroAngle, (float)gyroAngle,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(100);
        animation.setFillAfter(true);
        ivPointerGyro.startAnimation(animation);
        lastGyroAngle = gyroAngle;
    }

    public void setAimPoint(LatLng aimPoint){
        if (!aimPointList.contains(aimPoint)){
            aimPointList.add(aimPoint);
            MarkerOptions markerOptions = new MarkerOptions().position(aimPoint);
            markerOptions.title(String.valueOf(aimPointList.size()));
            markerOptions.snippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", aimPoint.latitude, aimPoint.longitude));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.aim)));
            markerOptions.anchor(0.5f, 0.5f);
            aMap.addMarker(markerOptions);
        }
    }

    private void resetMap(){
        aMap.clear();
        shipPointList.removeAll(shipPointList);
        shipPointList.add(new LatLng(0, 0));
        aimPointList.removeAll(aimPointList);
        markers.removeAll(markers);
        polylines.removeAll(polylines);
        initSmoothMove();
    }
}


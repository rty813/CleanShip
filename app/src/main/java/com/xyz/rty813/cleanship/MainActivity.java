package com.xyz.rty813.cleanship;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.os.Environment;
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
import android.widget.PopupWindow;
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
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.kcode.lib.UpdateWrapper;
import com.kcode.lib.bean.VersionModel;
import com.kcode.lib.net.CheckUpdateTask;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.xyz.rty813.cleanship.util.SerialPortTool;
import com.yanzhenjie.nohttp.NoHttp;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.download.DownloadRequest;
import com.yanzhenjie.nohttp.download.SimpleDownloadListener;
import com.yanzhenjie.nohttp.download.SyncDownloadExecutor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.dinus.com.loadingdrawable.LoadingView;
import lib.kingja.switchbutton.SwitchMultiButton;

public class MainActivity extends AppCompatActivity implements AMap.OnMapClickListener, AMap.OnMarkerClickListener, View.OnClickListener, GeocodeSearch.OnGeocodeSearchListener, SerialPortTool.onConnectedListener {
    private MapView mMapView;
    private AMap aMap;
    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;
    private ArrayList<Polyline> trace;
    public static SQLiteDBHelper dbHelper;
    private static SerialPortTool serialPort;
    private MorphingButton btn_start;
    private FloatingActionMenu fab_menu;
    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int GONE = 2;
    private static final int NAV = 3;
    private static final int NONE = -1;
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
    private SwitchMultiButton sw_nav;
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
    private int rfVersion = 0;
    private String rfSha256 = "";
    private String rfName = "";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("onCreate");
        MiStatInterface.initialize(this, MY_APPID, MY_APP_KEY, CHANNEL);
        MiStatInterface.setUploadPolicy(MiStatInterface.UPLOAD_POLICY_REALTIME, 0);
        MiStatInterface.enableExceptionCatcher(true);
        URLStatsRecorder.enableAutoRecord();
        NoHttp.initialize(this);
        dbHelper = new SQLiteDBHelper(this);
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        trace = new ArrayList<>();
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()){
                            MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
                            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE) ;
                            aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
                            aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
        serialPort = new SerialPortTool(this);
        serialPort.setListener(this);
        btn_start = findViewById(R.id.btn_start);
        fab_menu = findViewById(R.id.fab_menu);
        morph(state, 0);
        mHandler = new MyHandler();
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            private int index;
            @Override
            public void onMarkerDragStart(Marker marker) {
                for (index = 0; index < markers.size(); index++){
                    if (markers.get(index).hashCode() == marker.hashCode()){
                        break;
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                LatLng latLng = marker.getPosition();
                marker.setSnippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                if (markers.size() > 1){
                    PolylineOptions options = new PolylineOptions().width(10).color(Color.RED);
                    if (index == 0){
                        options.add(latLng, markers.get(1).getPosition());
                        polylines.get(0).setOptions(options);
                    }
                    else if (index == markers.size() - 1){
                        options.add(markers.get(markers.size() - 2).getPosition(), latLng);
                        polylines.get(polylines.size() - 1).setOptions(options);
                    }
                    else {
                        options.add(markers.get(index - 1).getPosition(), latLng);
                        polylines.get(index - 1).setOptions(options);
                        options = new PolylineOptions().width(10).color(Color.RED).add(latLng, markers.get(index + 1).getPosition());
                        polylines.get(index).setOptions(options);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
            }
        });
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
        findViewById(R.id.btn_trace).setOnClickListener(this);
        findViewById(R.id.fab_home).setOnClickListener(this);
        findViewById(R.id.fab_mark).setOnClickListener(this);
        findViewById(R.id.fab_ctrl).setOnClickListener(this);
        sw_nav = findViewById(R.id.sw_nav);
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
        new Thread(new QueryThread()).start();
        new Thread(new GetRFInfo()).start();
    }

    @Override
    public void onConnected() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("更新固件中");
        mHandler.sendEmptyMessage(10);
        new Thread(new CheckFirmwareUpdate()).start();
    }

    private class CheckFirmwareUpdate implements Runnable {
        @Override
        public void run() {
            try {
                serialPort.writeData("$VERSION#", 10);
                int lfVersion = Integer.parseInt(serialPort.readData()
                        .replace("\n", "")
                        .replace("\r", ""));
                if (lfVersion < rfVersion){
                    String url = "http://rty813.xyz/cleanship/bin/" + rfName;
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cleanship/";
                    DownloadRequest request = new DownloadRequest(url, RequestMethod.GET, path, true, true);
                    SyncDownloadExecutor.INSTANCE.execute(0, request, new SimpleDownloadListener() {
                        @Override
                        public void onFinish(int what, String filePath) {
                            try {
                                InputStream inputStream = new FileInputStream(filePath);
                                int size = inputStream.available();
                                byte[] buffer = new byte[size];
                                inputStream.read(buffer);
                                inputStream.close();
                                // 计算SHA-256 校验
                                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                                digest.update(buffer);
                                BigInteger bigInteger = new BigInteger(1, digest.digest());
                                Message msg = mHandler.obtainMessage(9);
                                if (!bigInteger.toString(16).equals(rfSha256)) {
                                    msg.obj = "校验错误！";
                                    mHandler.sendMessage(msg);
                                    return;
                                }
                                serialPort.writeData("y", 1000);
                                serialPort.writeByte(buffer, 0);
                                final String response = serialPort.readData();
                                if (response.contains("Success")) {
                                    msg.obj = "固件更新成功";
                                } else {
                                    msg.obj = "固件更新失败";
                                }
                                mHandler.sendMessage(msg);
                            } catch (IOException e){
                                e.printStackTrace();
                                mHandler.sendEmptyMessage(3);
                            } catch (Exception e) {
                                Message msg = mHandler.obtainMessage(9);
                                msg.obj = "固件更新失败";
                                mHandler.sendMessage(msg);
                                e.printStackTrace();
                            }
                        }
                    });
                }
                else{
                    synchronized (serialPort){
                        Message msg = mHandler.obtainMessage(9);
                        msg.obj = "已是最新版";
                        mHandler.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } catch (InterruptedException|NumberFormatException e) {
                e.printStackTrace();
                Message msg = mHandler.obtainMessage(9);
                msg.obj = "固件更新失败";
                mHandler.sendMessage(msg);
            }
        }
    }

    private class GetRFInfo implements Runnable{
        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("http://139.199.37.92/cleanship/bin.json").openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                final JSONObject json = new JSONObject(builder.toString());
                rfName = json.getString("name");
                rfVersion = json.getInt("version");
                rfSha256 = json.getString("sha256");
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
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

/**************************************临时测试**********************************************************************
 serialPort.writeData("$YAW#", 10);
 type = 2;
 ***************************************END!!!*************************************************************************/

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

    private class WriteSeiralThread implements Runnable{
        private final String mData;
        private final int mState;

        WriteSeiralThread(String data, int state){
            mData = data;
            mState = state;
        }

        @Override
        public void run() {
            try {
                do {
                    Thread.sleep(200);
                    serialPort.writeData(mData, 100);
                    String data = serialPort.readData();
                    if (data != null && data.contains(mData)){
                        Message msg = mHandler.obtainMessage();
                        msg.what = 8;
                        msg.obj = mState;
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
                    //Toast.makeText(MainActivity.this, "非法数据", Toast.LENGTH_SHORT).show();
                    break;
                case 6:
                    if (loadingView.getVisibility() == View.VISIBLE){
                        loadingView.startAnimation(animation);
                    }
                    break;
                case 7:
                    btn_start.setEnabled(true);
                    fab_menu.showMenuButton(true);
                    break;
                case 8:
                    synchronized (serialPort) {
                        if (loadingView.getVisibility() == View.VISIBLE){
                            loadingView.startAnimation(animation);
                        }
                        morph((Integer) msg.obj, 300);
                        serialPort.notify();
                    }
                    break;
                case 9:
                    synchronized (serialPort){
                        Toast.makeText(MainActivity.this, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        morph(READY, 200);
                        serialPort.notify();
                    }
                    break;
                case 10:
                    progressDialog.show();
                    break;
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
                .setUrl("http://rty813.xyz/cleanship/app.json")
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
        serialPort.unregisterReceiver(this);
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
        markerOptions.setFlat(true);
        markerOptions.draggable(true);

        System.out.println(latLng.toString());
        markers.add(aMap.addMarker(markerOptions));
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
            markerOptions.setFlat(true);
            markerOptions.draggable(true);
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
//                            使QueryThread进入Wait
                            state = UNREADY;
                            btn_start.setEnabled(false);
                            fab_menu.hideMenuButton(false);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        for (int i = 0; i < markers.size(); i++){
                                            double latitude = markers.get(i).getPosition().latitude;
                                            double longitude = markers.get(i).getPosition().longitude;
                                            if (!(sw_nav.getSelectedTab() == 1 && i == markers.size() - 1
                                                    && latitude == markers.get(0).getPosition().latitude
                                                    && longitude == markers.get(0).getPosition().longitude)){
                                                serialPort.writeData(String.format(Locale.getDefault(),
                                                        "$GNGGA,%.6f,%.6f#",latitude, longitude), 300);
                                            }
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
                        fab_menu.hideMenuButton(false);
                        String data = sw_nav.getSelectedTab() == 0? "$NAV,1#": "$NAV,2#";
                        new Thread(new WriteSeiralThread(data, GONE)).start();
                        break;
                    case GONE:
                        state = UNREADY;
                        showLoadingView();
                        btn_start.setEnabled(false);
                        fab_menu.hideMenuButton(false);
                        new Thread(new WriteSeiralThread("$STOP#", READY)).start();
                        break;
                }
                break;
            case R.id.fab_home:
                if (state != UNREADY){
                    fab_menu.close(true);
                    int pre_state = state;
                    state = UNREADY;
                    showLoadingView();
                    btn_start.setEnabled(false);
                    fab_menu.hideMenuButton(false);
                    new Thread(new WriteSeiralThread("$ORDER,2#", pre_state)).start();
                }
                break;
            case R.id.fab_mark:
                if (state != UNREADY && markers.size() == 1){
                    fab_menu.close(true);
                    int pre_state = state;
                    state = UNREADY;
                    showLoadingView();
                    btn_start.setEnabled(false);
                    fab_menu.hideMenuButton(false);
                    new Thread(new WriteSeiralThread(String.format(Locale.getDefault(),
                            "$ORDER,1,%.6f,%.6f#", markers.get(0).getPosition().latitude,
                            markers.get(0).getPosition().longitude), pre_state)).start();
                }
                break;
            case R.id.fab_ctrl:
                startActivity(new Intent(MainActivity.this, MapActivity.class));
                if (state != UNREADY){
//                    morph(UNREADY, 0);
//                    Intent intent = new Intent(this, ControllerActivity.class);
//                    startActivity(intent);
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
            case R.id.btn_trace:
                for (Polyline aTrace : trace){
                    aTrace.setVisible(!aTrace.isVisible());
                }
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
        if (state != NONE){
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
                markerOptions.draggable(true);
                markerOptions.setFlat(true);
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
            trace.add(aMap.addPolyline(new PolylineOptions().add(shipPointList.get(shipPointList.size() - 2),
                    shipPointList.get(shipPointList.size() - 1)).width(5).color(Color.rgb(61, 110, 234))));
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
            markerOptions.draggable(true);
            markerOptions.setFlat(true);
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
        trace.removeAll(trace);
        initSmoothMove();
    }

    public static SerialPortTool getSerialPort() {
        return serialPort;
    }
}


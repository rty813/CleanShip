package com.xyz.rty813.cleanship;

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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.xyz.rty813.cleanship.util.SerialPortTool;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import app.dinus.com.loadingdrawable.LoadingView;
import lib.kingja.switchbutton.SwitchMultiButton;

public class MapActivity extends AppCompatActivity implements View.OnClickListener, AMap.OnMapClickListener, AMap.OnMarkerClickListener, GeocodeSearch.OnGeocodeSearchListener {

    private AMap aMap;
    private MapView mMapView;
    private long mExitTime;
    private PopupWindow popupWindow;
    private SerialPortTool serialPort;
    private int state;

    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int GONE = 2;
    private static final int NAV = 3;
    private static final int NONE = -1;
    private static final int PAUSE = 4;

    private MyHandler mHandler;
    private String pos = null;
    private LoadingView loadingView;
    private MyReceiver receiver;
    private ArrayList<LatLng> shipPointList;
    private ArrayList<LatLng> aimPointList;
    private SmoothMoveMarker smoothMoveMarker;
    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;
    private ArrayList<Polyline> trace;
    public static SQLiteDBHelper dbHelper;

    private TextView tvAimAngle;
    private TextView tvGyroAngle;
    private TextView tvCurrVel;
    private TextView tvCurrLat;
    private TextView tvCurrLng;
    private TextView tvRawData;
    private TextView tvGpsNum;
    private ImageView ivPointerAim;
    private ImageView ivPointerGyro;
    private double lastAimAngle;
    private double lastGyroAngle;
    private float alpha = 1.0f;
    private Button btnGostop;
    private Button btnCalc;
    private Drawable picPause;
    private Drawable picStart;
    private Drawable picCalc;
    private Drawable picNav;
    private Drawable picWorking;
    private SwitchMultiButton sw_nav;
    private Button btnRoute;
    private ImageButton btnMore;

    private static final String MY_APPID = "2882303761517676503";
    private static final String MY_APP_KEY = "5131767662503";
    private static final String CHANNEL = "SELF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_map);
        MiStatInterface.initialize(this, MY_APPID, MY_APP_KEY, CHANNEL);
        MiStatInterface.setUploadPolicy(MiStatInterface.UPLOAD_POLICY_REALTIME, 0);
        MiStatInterface.enableExceptionCatcher(true);
        URLStatsRecorder.enableAutoRecord();

        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE) ;
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
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
        initView();
        mHandler = new MyHandler(this);
        dbHelper = new SQLiteDBHelper(this);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        trace = new ArrayList<>();
        aimPointList = new ArrayList<>();
        initSmoothMove();
        state = ConnectActivity.getState();
        serialPort = ConnectActivity.getSerialPort();
        serialPort.registerReceiver(this);
//        serialPort = new SerialPortTool(this);
        new Thread(new QueryThread()).start();
    }

    private void initView() {
        loadingView = findViewById(R.id.loadingview);

        tvAimAngle = findViewById(R.id.tv_aim_angle);
        tvCurrVel = findViewById(R.id.tv_curr_vel);
        tvGyroAngle = findViewById(R.id.tv_gyro_angle);
        tvCurrLat = findViewById(R.id.tv_curr_lat);
        tvCurrLng = findViewById(R.id.tv_curr_lng);
        tvRawData = findViewById(R.id.tv_raw_data);
        tvGpsNum = findViewById(R.id.tv_gps_num);
        ivPointerAim = findViewById(R.id.ivPointerAim);
        ivPointerGyro = findViewById(R.id.ivPointerGyro);
        sw_nav = findViewById(R.id.sw_nav);
        btnGostop = findViewById(R.id.btn_gostop);
        btnCalc = findViewById(R.id.btn_calc);
        btnRoute = findViewById(R.id.btn_route);
        btnMore = findViewById(R.id.btn_more);
        picStart = getResources().getDrawable(R.drawable.start);
        picPause = getResources().getDrawable(R.drawable.pause);
        picCalc = getResources().getDrawable(R.drawable.calc);
        picNav = getResources().getDrawable(R.drawable.new_nav);
        picWorking = getResources().getDrawable(R.drawable.working);

        picStart.setBounds(0, 0, picStart.getMinimumWidth(), picStart.getMinimumHeight());
        picPause.setBounds(0, 0, picPause.getMinimumWidth(), picPause.getMinimumHeight());
        picCalc.setBounds(0, 0, picCalc.getMinimumWidth(), picCalc.getMinimumHeight());
        picNav.setBounds(0, 0, picNav.getMinimumWidth(), picNav.getMinimumHeight());
        picWorking.setBounds(0, 0, picWorking.getMinimumWidth(), picWorking.getMinimumHeight());

        findViewById(R.id.btn_more).setOnClickListener(this);
        findViewById(R.id.btn_disconnect).setOnClickListener(this);
        findViewById(R.id.btn_route).setOnClickListener(this);
        findViewById(R.id.btn_gostop).setOnClickListener(this);
        findViewById(R.id.btn_calc).setOnClickListener(this);
        findViewById(R.id.fab_plane).setOnClickListener(this);
        findViewById(R.id.fab_satellite).setOnClickListener(this);
    }

    private class QueryThread implements Runnable {
        @Override
        public void run() {
            synchronized (serialPort){
                int type = 0;
                boolean err = false;
                while (true) {
                    String data = null;
                    try {
                        if (state == UNREADY){
                            serialPort.wait();
                        }
                        if (!err){
                            serialPort.writeData(String.format(Locale.getDefault(), "$QUERY,%d#", type), 10);
                        }
                        StringBuilder builder = new StringBuilder();
                        while (!(data = serialPort.readData()).equals("")){
                            builder.append(data);
                            if (data.endsWith("#")){
                                break;
                            }
                        }
                        data = builder.toString();
                        if (!data.startsWith("$") || !data.endsWith("#")){
                            err = false;
                            Thread.sleep(400);
                            continue;
                        }
                        else{
                            data = data.replaceAll("#", "");
                            data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                        }

                        if (!data.equals("")) {
                            String[] strings = data.split(";");
                            Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                            if (strings.length == 2){
                                intent.putExtra("rawData", String.format(Locale.getDefault(),
                                        "|%d|%s|%s", type, strings[0], strings[1]));
                            }
                            else{
                                intent.putExtra("rawData", data);
                            }
                            if (strings.length == 2 && Integer.parseInt(strings[0]) < 6) {
                                intent.putExtra("type", Integer.parseInt(strings[0]));
                                intent.putExtra("data", strings[1]);
                            }
                            if (strings.length == 2 && Integer.parseInt(strings[0]) == type) {
                                type = (type + 1) % 6;
                                err = false;
                            }
                            else if (strings.length == 2){
                                err = true;
                            }
                            sendBroadcast(intent);
                        }
                        else{
                            err = false;
                        }
                        Thread.sleep(400);
                    } catch (NumberFormatException | InterruptedException e) {
                        Message msg = mHandler.obtainMessage(5);
                        msg.obj = data;
                        mHandler.sendMessage(msg);
                        e.printStackTrace();
                    } catch (IOException e){
                        e.printStackTrace();
                        mHandler.sendEmptyMessage(3);
                    }
                }
            }
        }
    }

    private class WriteSerialThread implements Runnable{
        private final String mData;
        private final int mState;
        WriteSerialThread(String data, int state){
            mData = data;
            mState = state;
        }
        @Override
        public void run() {
            try {
                do {
                    serialPort.writeData(mData, 10);
                    String data;
                    StringBuilder builder = new StringBuilder();
                    while (!(data = serialPort.readData()).equals("")){
                        builder.append(data);
                        if (data.endsWith("#")){
                            break;
                        }
                    }
                    data = builder.toString();
                    Log.i("writeSerialPort", data);
                    if (data.contains(mData)){
                        Message msg = mHandler.obtainMessage(8);
                        msg.obj = mState;
                        mHandler.sendMessage(msg);
                        break;
                    }
                    Thread.sleep(400);
                } while (true);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } finally {
                mHandler.sendEmptyMessage(7);
            }
        }
    }

    static class MyHandler extends Handler {
        WeakReference<MapActivity> mActivity;
        MyHandler(MapActivity activity){
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            final MapActivity activity = mActivity.get();
            AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
            animation.setDuration(300);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    activity.loadingView.setVisibility(View.GONE);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            switch (msg.what){
                case 1:
                    WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
                    lp.alpha = (float) msg.obj;
                    activity.getWindow().setAttributes(lp);
                    break;
                case 2:
                    ((PopupWindow)msg.obj).showAtLocation(activity.mMapView, Gravity.CENTER, 0, 0);
                    break;
                case 3:
                    if (activity.loadingView.getVisibility() == View.VISIBLE){
                        activity.loadingView.startAnimation(animation);
                    }
                    activity.morph(UNREADY, 200);
                    break;
                case 4:
                    if (activity.loadingView.getVisibility() == View.VISIBLE){
                        activity.loadingView.startAnimation(animation);
                    }
                    synchronized (activity.serialPort){
                        activity.morph(NAV, 300);
                        activity.serialPort.notify();
                    }
                    break;
                case 5:
                    Log.d("非法数据", (String) msg.obj);
                    break;
                case 6:
                    if (activity.loadingView.getVisibility() == View.VISIBLE){
                        activity.loadingView.startAnimation(animation);
                    }
                    break;
                case 7:
//                    btn_start.setEnabled(true);
                    break;
                case 8:
                    synchronized (activity.serialPort) {
                        if (activity.loadingView.getVisibility() == View.VISIBLE){
                            activity.loadingView.startAnimation(animation);
                        }
                        activity.morph((Integer) msg.obj, 300);
                        activity.serialPort.notify();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int preState = state;
        switch (view.getId()) {
            case R.id.btn_more:
                View contentView = LayoutInflater.from(this).inflate(R.layout.menu_more, null);
                contentView.findViewById(R.id.menu_btn_home).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_help).setOnClickListener(this);
//                临时测试遥控器功能
                contentView.findViewById(R.id.menu_btn_rc).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_trace).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_clear).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_order5).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_nav).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_go).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_stop).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_startquery).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_closequery).setOnClickListener(this);
                popupWindow = new PopupWindow();
                popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popupWindow.setOutsideTouchable(true);
                popupWindow.setFocusable(true);
                popupWindow.setContentView(contentView);
                popupWindow.setAnimationStyle(R.style.dismiss_anim);
                popupWindow.showAsDropDown(findViewById(R.id.btn_more));
                break;
            case R.id.btn_route:
                contentView = LayoutInflater.from(this).inflate(R.layout.menu_route, null);
                contentView.findViewById(R.id.menu_btn_cancel).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_delete).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_detail).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_history).setOnClickListener(this);
                contentView.findViewById(R.id.menu_btn_mark).setOnClickListener(this);

                contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int popupHeight = contentView.getMeasuredHeight();
                int popupWidth = contentView.getMeasuredWidth();
                int[] location = new int[2];
                findViewById(R.id.btn_route).getLocationOnScreen(location);

                popupWindow = new PopupWindow();
                popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popupWindow.setOutsideTouchable(true);
                popupWindow.setFocusable(true);
                popupWindow.setContentView(contentView);
                popupWindow.setAnimationStyle(R.style.bottom_menu_anim);
                popupWindow.showAtLocation(findViewById(R.id.btn_route),
                        Gravity.NO_GRAVITY, (int)(location[0] + popupWidth * 0.2), location[1] - popupHeight);
                break;
            case R.id.btn_disconnect:
                startActivity(new Intent(this, ConnectActivity.class));
                finish();
                break;
            case R.id.menu_btn_help:
                popupWindow.dismiss();
                Toast.makeText(MapActivity.this, "帮助", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_btn_home:
                if (state != UNREADY || true){
                    popupWindow.dismiss();
                    state = UNREADY;
                    showLoadingView();
//                    btn_start.setEnabled(false);
                    new Thread(new WriteSerialThread("$ORDER,2#", preState)).start();
                }
                break;
            case R.id.menu_btn_cancel:
                if (markers.size() > 0) {
                    popupWindow.dismiss();
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
            case R.id.menu_btn_delete:
                popupWindow.dismiss();
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
            case R.id.menu_btn_detail:
                if (markers.size() > 0){
                    popupWindow.dismiss();
                    final View popupview = View.inflate(MapActivity.this,R.layout.popupwindow_detail,null);
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
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MapActivity.this);
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
            case R.id.menu_btn_history:
                popupWindow.dismiss();
                startActivityForResult(new Intent(this, HistoryActivity.class), 200);
                break;
            case R.id.menu_btn_mark:
                if (state != UNREADY && markers.size() == 1){
                    popupWindow.dismiss();
                    state = UNREADY;
                    showLoadingView();
//                    btn_start.setEnabled(false);
                    new Thread(new WriteSerialThread(String.format(Locale.getDefault(),
                            "$ORDER,1,%.6f,%.6f#", markers.get(0).getPosition().latitude,
                            markers.get(0).getPosition().longitude), preState)).start();
                }
                break;
            case R.id.menu_btn_rc:
                popupWindow.dismiss();
                state = UNREADY;
                startActivity(new Intent(this, ControllerActivity.class));
                break;
            case R.id.fab_plane:
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                break;
            case R.id.fab_satellite:
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.menu_btn_trace:
                popupWindow.dismiss();
                for (Polyline aTrace : trace){
                    aTrace.setVisible(!aTrace.isVisible());
                }
                break;
            case R.id.btn_gostop:
                if (state == PAUSE){
                    state = UNREADY;
                    showLoadingView();
                    new Thread(new WriteSerialThread("$GO#", GONE)).start();
                }
                else if (state == GONE){
                    state = UNREADY;
                    showLoadingView();
                    new Thread(new WriteSerialThread("$STOP#", PAUSE)).start();
                }
                break;
            case R.id.btn_calc:
                if (state == READY || state == UNREADY){
                    if (markers.size() > 0){
                        StringBuilder stringBuilder = new StringBuilder();
                        for (Marker marker : markers){
                            stringBuilder.append(String.format(Locale.getDefault(), "%.6f,%.6f;", marker.getPosition().latitude, marker.getPosition().longitude));
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
                        saveRoute(dateFormat.format(new Date(System.currentTimeMillis())), stringBuilder.toString(), pos);
                        showLoadingView();
//                            使QueryThread进入Wait
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    serialPort.writeData("$CLEAR#", 1000);
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
                }
                else if (state == NAV){
                    state = UNREADY;
                    showLoadingView();
                    String data = sw_nav.getSelectedTab() == 0? "$NAV,1#": "$NAV,2#";
                    new Thread(new WriteSerialThread(data, GONE)).start();
                }
                else if (state == GONE){
                    state = UNREADY;
                    showLoadingView();
                    new Thread(new WriteSerialThread("$STOP#", READY)).start();
                }
                break;
            case R.id.menu_btn_clear:
                state = UNREADY;
                showLoadingView();
                new Thread(new WriteSerialThread("$CLEAR#", READY)).start();
                break;
            case R.id.menu_btn_order5:
                state = UNREADY;
                showLoadingView();
                new Thread(new WriteSerialThread("$ORDER,5#", preState)).start();
                break;
            case R.id.menu_btn_nav:
                state = UNREADY;
                showLoadingView();
                String data = sw_nav.getSelectedTab() == 0? "$NAV,1#": "$NAV,2#";
                new Thread(new WriteSerialThread(data, preState)).start();
                break;
            case R.id.menu_btn_go:
                state = UNREADY;
                showLoadingView();
                new Thread(new WriteSerialThread("$GO#", preState)).start();
                break;
            case R.id.menu_btn_stop:
                state = UNREADY;
                btnCalc.setEnabled(false);
                btnGostop.setEnabled(false);
                showLoadingView();
                new Thread(new WriteSerialThread("$STOP#", preState)).start();
                break;
            case R.id.menu_btn_startquery:
                Toast.makeText(this, "开始问询", Toast.LENGTH_SHORT).show();
                synchronized (serialPort){
                    state = READY;
                    serialPort.notify();
                }
                break;
            case R.id.menu_btn_closequery:
                state = UNREADY;
                Toast.makeText(this, "停止问询", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showLoadingView() {
        state = UNREADY;
        btnGostop.setEnabled(false);
        btnCalc.setEnabled(false);
        btnRoute.setEnabled(false);
        findViewById(R.id.loadingview).setVisibility(View.VISIBLE);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        findViewById(R.id.loadingview).startAnimation(animation);
    }

    private void morph(int state, int duration){
        if (state != NONE){
            this.state = state;
            btnCalc.setEnabled(true);
            btnGostop.setEnabled(true);
            btnRoute.setEnabled(true);
            btnMore.setEnabled(true);
        }
        else{
            this.state = UNREADY;
            btnCalc.setEnabled(false);
            btnGostop.setEnabled(false);
            btnMore.setEnabled(false);
            btnRoute.setEnabled(false);
        }
        switch (state){
            case UNREADY:
                Toast.makeText(this, "连接中断，请重新连接", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ConnectActivity.class));
                finish();
                break;
            case READY:
                btnGostop.setText("开始");
                btnGostop.setCompoundDrawables(null, picStart, null, null);
                btnGostop.setEnabled(false);
                btnCalc.setText("计算");
                btnCalc.setCompoundDrawables(null, picCalc, null, null);
                break;
            case NAV:
                btnGostop.setText("开始");
                btnGostop.setCompoundDrawables(null, picStart, null, null);
                btnGostop.setEnabled(false);
                btnCalc.setText("导航");
                btnCalc.setCompoundDrawables(null, picNav, null, null);
                break;
            case GONE:
                btnGostop.setText("暂停");
                btnGostop.setCompoundDrawables(null, picPause, null, null);
                btnGostop.setEnabled(true);
                btnCalc.setText("正在导航");
                btnCalc.setCompoundDrawables(null, picWorking, null, null);
                break;
            case PAUSE:
                btnGostop.setText("开始");
                btnGostop.setCompoundDrawables(null, picStart, null, null);
                btnGostop.setEnabled(true);
                btnCalc.setText("正在导航");
                btnCalc.setCompoundDrawables(null, picWorking, null, null);
                break;
        }
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
        SQLiteDatabase database = dbHelper.getReadableDatabase();
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

    public void setCurrVel(String currVel) {
        this.tvCurrVel.setText(currVel);
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

    public void setAimPoint(LatLng aimPoint) {
        if (!aimPointList.contains(aimPoint)) {
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

    @Override
    protected void onPause() {
        mMapView.onPause();
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        receiver = new MyReceiver();
        registerReceiver(receiver, new IntentFilter(MyReceiver.ACTION_DATA_RECEIVED));
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        state = UNREADY;
        mMapView.onDestroy();
        serialPort.closeDevice();
        serialPort.unregisterReceiver(this);
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mMapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mMapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(MapActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

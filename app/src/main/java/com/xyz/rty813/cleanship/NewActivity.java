package com.xyz.rty813.cleanship;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.util.DisplayMetrics;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
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
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.kcode.lib.UpdateWrapper;
import com.kcode.lib.bean.VersionModel;
import com.kcode.lib.net.CheckUpdateTask;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.xyz.rty813.cleanship.util.SerialPortTool;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;
import com.yanzhenjie.recyclerview.swipe.SwipeItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenu;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import app.dinus.com.loadingdrawable.LoadingView;
import lib.kingja.switchbutton.SwitchMultiButton;

public class NewActivity extends AppCompatActivity implements View.OnClickListener, SerialPortTool.onConnectedListener {
    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int GONE = 2;
    private static final int NAV = 3;
    private static final int NONE = -1;
    private static final int HOMING = 5;
    private static final int PAUSE = 4;
    private static final String MY_APPID = "2882303761517676503";
    private static final String MY_APP_KEY = "5131767662503";
    private static final String CHANNEL = "SELF";
    private static final int BAUD_RATE = 115200;

    public static SQLiteDBHelper dbHelper;
    private AMap aMap;
    private MapView mMapView;
    private long mExitTime;
    private SerialPortTool serialPort;
    private int state;
    private boolean markEnable = false;
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
    private float alpha = 1.0f;
    private Button btnConnect;
    private LinearLayout ll_finish;
    private LinearLayout ll_nav;
    private LinearLayout ll_mark;
    private LinearLayout ll_method;
    private SwitchMultiButton sw_nav;
    private Button btnHome;
    private Button btnHome2;
    private Button btnGostop;
    private AppCompatSeekBar seekBar;
    private Button btnVel;
    private Button btnHistory;
    private Button btnManual;
    private Button btnAbort;
    private Drawable picPause;
    private Drawable picStart;
    private Drawable picMarkEnable;
    private Drawable picMarkDisable;
    private Button btnEnable;
    private TextView tvToolbar;
    private TextView tvCircle;
    private Circle limitCircle;
    private static final double ctlRadius = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        MiStatInterface.initialize(this, MY_APPID, MY_APP_KEY, CHANNEL);
        MiStatInterface.setUploadPolicy(MiStatInterface.UPLOAD_POLICY_REALTIME, 0);
        MiStatInterface.enableExceptionCatcher(true);
        URLStatsRecorder.enableAutoRecord();
        initView(savedInstanceState);
        initAMap();
        initClass();
        requestPermission();
    }

    @Override
    protected void onResume() {
        MiStatInterface.recordPageStart(NewActivity.this, "主界面");
        mMapView.onResume();
        receiver = new MyReceiver();
        registerReceiver(receiver, new IntentFilter(MyReceiver.ACTION_DATA_RECEIVED));
        super.onResume();
    }

    @Override
    protected void onPause() {
        MiStatInterface.recordPageEnd();
        mMapView.onPause();
        unregisterReceiver(receiver);
        super.onPause();
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
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(NewActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initClass() {
        mHandler = new MyHandler(this);
        dbHelper = new SQLiteDBHelper(this);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        trace = new ArrayList<>();
        aimPointList = new ArrayList<>();
        initSmoothMove();
        state = UNREADY;
        serialPort = new SerialPortTool(this);
        serialPort.registerReceiver(this);
        serialPort.setListener(this);
    }

    private void initView(Bundle savedInstanceState) {
        loadingView = findViewById(R.id.loadingview);
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);

        btnConnect = findViewById(R.id.btn_connect);
        btnHome = findViewById(R.id.btn_home);
        ll_nav = findViewById(R.id.ll_nav);
        ll_mark = findViewById(R.id.ll_mark);
        ll_method = findViewById(R.id.ll_method);
        ll_finish = findViewById(R.id.ll_finish);
        sw_nav = findViewById(R.id.sw_nav);
        btnGostop = findViewById(R.id.btn_gostop);
        btnHome2 = findViewById(R.id.btn_home2);
        btnHistory = findViewById(R.id.btn_history);
        btnManual = findViewById(R.id.btn_manual);
        btnVel = findViewById(R.id.btn_vel);
        btnAbort = findViewById(R.id.btn_abort);
        btnEnable = findViewById(R.id.btn_enable);
        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                btnVel.setVisibility(View.VISIBLE);
                btnHome2.setVisibility(View.VISIBLE);
                btnGostop.setVisibility(View.VISIBLE);
                btnAbort.setVisibility(View.VISIBLE);
                seekBar.setVisibility(View.GONE);
                new Thread(new WriteSerialThread(String.format(Locale.getDefault(),
                        "$ORDER,6,%d#", seekBar.getProgress()), NONE, state)).start();
            }
        });
        tvToolbar = findViewById(R.id.tv_toolbar);
        tvCircle = findViewById(R.id.tv_circle);

        findViewById(R.id.fab_plane).setOnClickListener(this);
        findViewById(R.id.fab_satellite).setOnClickListener(this);
        findViewById(R.id.btn_connect).setOnClickListener(this);
        findViewById(R.id.btn_go).setOnClickListener(this);
        findViewById(R.id.btn_enable).setOnClickListener(this);
        findViewById(R.id.btn_delete).setOnClickListener(this);
        findViewById(R.id.btn_vel).setOnClickListener(this);
        findViewById(R.id.btn_home2).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        btnAbort.setOnClickListener(this);
        btnManual.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
        btnHome.setOnClickListener(this);
        btnGostop.setOnClickListener(this);
        btnEnable.setOnClickListener(this);

        picStart = getResources().getDrawable(R.drawable.btn_start_selector);
        picPause = getResources().getDrawable(R.drawable.btn_pause_selector);
        picMarkEnable = getResources().getDrawable(R.drawable.mark_y);
        picMarkDisable = getResources().getDrawable(R.drawable.mark_n);
        picStart.setBounds(0, 0, picStart.getMinimumWidth(), picStart.getMinimumHeight());
        picPause.setBounds(0, 0, picPause.getMinimumWidth(), picPause.getMinimumHeight());
        picMarkDisable.setBounds(0, 0, picMarkDisable.getMinimumWidth(), picMarkDisable.getMinimumHeight());
        picMarkEnable.setBounds(0, 0, picMarkEnable.getMinimumWidth(), picMarkEnable.getMinimumHeight());
    }

    private void initAMap() {
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        aMap.getUiSettings().setCompassEnabled(false);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            private int i;

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
                    Toast.makeText(NewActivity.this, "完成闭合回路！", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            private int index;

            @Override
            public void onMarkerDragStart(Marker marker) {
                for (index = 0; index < markers.size(); index++) {
                    if (markers.get(index).hashCode() == marker.hashCode()) {
                        break;
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                LatLng latLng = marker.getPosition();
                marker.setSnippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                if (markers.size() > 1) {
                    PolylineOptions options = new PolylineOptions().width(10).color(Color.RED);
                    if (index == 0) {
                        options.add(latLng, markers.get(1).getPosition());
                        polylines.get(0).setOptions(options);
                    } else if (index == markers.size() - 1) {
                        options.add(markers.get(markers.size() - 2).getPosition(), latLng);
                        polylines.get(polylines.size() - 1).setOptions(options);
                    } else {
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
        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            private int i;

            @Override
            public void onMapClick(LatLng latLng) {
                if (!markEnable) {
                    return;
                }
                if (AMapUtils.calculateLineDistance(latLng, limitCircle.getCenter()) > ctlRadius){
                    Toast.makeText(NewActivity.this, "注意！超出遥控范围！", Toast.LENGTH_LONG).show();
                }
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
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(10)
                            .color(Color.parseColor("#0B76CE"))));
                } else {
                    GeocodeSearch geocodeSearch = new GeocodeSearch(NewActivity.this);
                    geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
                        @Override
                        public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                            if (i == 1000) {
                                RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
                                pos = address.getProvince();
                                if (!address.getProvince().equals(address.getCity())) {
                                    pos += " " + address.getCity();
                                }
                                pos += " " + address.getDistrict();
                                if (address.getPois().size() != 0) {
                                    pos = pos + " " + address.getPois().get(0);
                                }
                                System.out.println(pos);
                            }
                        }

                        @Override
                        public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

                        }
                    });
                    RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 1000, GeocodeSearch.AMAP);
                    geocodeSearch.getFromLocationAsyn(query);
                }
            }
        });
        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (limitCircle != null){
                    limitCircle.remove();
                }
                limitCircle = aMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(), location.getLongitude()))
                        .radius(ctlRadius).strokeColor(Color.RED).strokeWidth(8).fillColor(Color.argb(20, 1, 1, 1)));

            }
        });
    }

    private void initSerialPort() {
        System.out.println("init serial port");
        List<UsbSerialDriver> list = serialPort.searchSerialPort();
        if (list.isEmpty()) {
            mHandler.sendEmptyMessage(3);
            mHandler.sendMessage(mHandler.obtainMessage(9, "未连接设备"));
        } else {
            try {
                serialPort.initDevice(list.get(0), BAUD_RATE);
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
                mHandler.sendMessage(mHandler.obtainMessage(9, "连接失败"));
            }
        }
    }

    @Override
    public void onClick(View view) {
        int preState = state;
        PopupWindow popupHistory;
        switch (view.getId()) {
            case R.id.btn_connect:
                showLoadingView();
                initSerialPort();
                break;
            case R.id.btn_delete:
                new AlertDialog.Builder(this).setTitle("提示")
                        .setMessage("是否要清除所有标记？")
                        .setNegativeButton("否", null)
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                resetMap();
                            }
                        })
                        .show();
                break;
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
            case R.id.btn_home:
            case R.id.btn_home2:
                if (state != UNREADY) {
                    new Thread(new WriteSerialThread("$ORDER,2#", GONE, state)).start();
                }
                break;
            case R.id.btn_enable:
                markEnable = !markEnable;
                btnEnable.setCompoundDrawables(null, markEnable? picMarkEnable : picMarkDisable, null, null);
                break;
            case R.id.btn_go:
                if (state == READY) {
                    if (markers.size() > 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (Marker marker : markers) {
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
                                    for (int i = 0; i < markers.size(); i++) {
                                        double latitude = markers.get(i).getPosition().latitude;
                                        double longitude = markers.get(i).getPosition().longitude;
                                        if (!(sw_nav.getSelectedTab() == 1 && i == markers.size() - 1
                                                && latitude == markers.get(0).getPosition().latitude
                                                && longitude == markers.get(0).getPosition().longitude)) {
                                            serialPort.writeData(String.format(Locale.getDefault(),
                                                    "$GNGGA,%.6f,%.6f#", latitude, longitude), 500);
                                        }
                                    }
                                    String data = sw_nav.getSelectedTab() == 0? "$NAV,1#": "$NAV,2#";
                                    new Thread(new WriteSerialThread(data, GONE, READY)).start();
//                                    serialPort.writeData(data, 500);
//                                    mHandler.sendMessage(mHandler.obtainMessage(8, GONE));
                                } catch (InterruptedException | IOException e) {
                                    e.printStackTrace();
                                    mHandler.sendEmptyMessage(3);
                                } finally {
                                    mHandler.sendEmptyMessage(7);
                                }
                            }
                        }).start();
                    } else {
                        loadRoute(null);
                    }
                }
                break;
            case R.id.btn_vel:
                btnGostop.setVisibility(View.GONE);
                btnAbort.setVisibility(View.GONE);
                btnHome2.setVisibility(View.GONE);
                btnVel.setVisibility(View.GONE);
                seekBar.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_history:
                btnManual.setTextColor(Color.BLACK);
                btnHistory.setTextColor(getResources().getColor(R.color.toolbarBlue));
                final View contentView = LayoutInflater.from(this).inflate(R.layout.popup_history, null);
                final SwipeMenuRecyclerView recyclerView = contentView.findViewById(R.id.recyclerView);
                final TextView textView = contentView.findViewById(R.id.tv_history);
                popupHistory = new PopupWindow();
                popupHistory.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
                popupHistory.setOutsideTouchable(true);
                popupHistory.setContentView(contentView);
                popupHistory.setAnimationStyle(R.style.dismiss_anim);
                popupHistory.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        btnHistory.setTextColor(Color.BLACK);
                        btnManual.setTextColor(getResources().getColor(R.color.toolbarBlue));
                    }
                });
                loadHistory(recyclerView, textView, popupHistory);
                contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                final int height = getResources().getDisplayMetrics().heightPixels / 2;
                if (contentView.getMeasuredHeight() > height){
                    popupHistory.setHeight(height);
                }
                else {
                    popupHistory.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                popupHistory.showAsDropDown(findViewById(R.id.ll_method));
                break;
            case R.id.btn_abort:
                new Thread(new WriteSerialThread("$CLEAR#", READY, state)).start();
                break;
            case R.id.btn_gostop:
                if (state == PAUSE){
                    new Thread(new WriteSerialThread("$GO#", GONE, state)).start();
                }
                else if (state == GONE){
                    new Thread(new WriteSerialThread("$STOP#", PAUSE, state)).start();
                }
                break;
        }
    }

    private void loadHistory(SwipeMenuRecyclerView recyclerView, TextView textView, final PopupWindow popupHistory) {
        final ArrayList<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, null, null ,null, null, null);
        if (cursor.getCount() > 0){
            cursor.moveToLast();
            do{
                Map<String, String> map = new HashMap();
                String time = cursor.getString(cursor.getColumnIndex("TIME"));
                String name = cursor.getString(cursor.getColumnIndex("NAME"));
                String id = cursor.getString(cursor.getColumnIndex("ID"));
                map.put("detail", time);
                map.put("title", name);
                map.put("id", id);
                list.add(map);
            } while(cursor.moveToPrevious());
        }
        database.close();
        final SwipeRecyclerViewAdapter adapter = new SwipeRecyclerViewAdapter(list);
        adapter.notifyDataSetChanged();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setSwipeItemClickListener(new SwipeItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                popupHistory.dismiss();
                loadRoute(list.get(position).get("id"));
            }
        });
        recyclerView.setSwipeMenuCreator(new SwipeMenuCreator() {
            @Override
            public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
                DisplayMetrics metrics = NewActivity.this.getResources().getDisplayMetrics();
                SwipeMenuItem deleteItem = new SwipeMenuItem(NewActivity.this).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_delete).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);

                SwipeMenuItem renameItem = new SwipeMenuItem(NewActivity.this).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_rename).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                swipeRightMenu.addMenuItem(renameItem);
                swipeRightMenu.addMenuItem(deleteItem);
            }
        });
        recyclerView.setSwipeMenuItemClickListener(new SwipeMenuItemClickListener() {
            @Override
            public void onItemClick(SwipeMenuBridge menuBridge) {
                menuBridge.closeMenu();
                final int pos = menuBridge.getAdapterPosition();
                if (menuBridge.getPosition() == 1){
                    Map<String, String> map = list.get(pos);
                    String id = map.get("id");
                    list.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    adapter.notifyItemRangeChanged(pos, list.size() - pos);
                    SQLiteDatabase database = dbHelper.getWritableDatabase();
                    database.delete(SQLiteDBHelper.TABLE_NAME, "ID=?", new String[]{id});
                    database.close();
                }
                else {
                    final EditText etName = new EditText(NewActivity.this);
                    etName.setHint(list.get(pos).get("title"));
                    new AlertDialog.Builder(NewActivity.this)
                            .setTitle("重命名路线")
                            .setView(etName)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Map<String, String> map = new HashMap<>();
                                    map.put("id", list.get(pos).get("id"));
                                    map.put("title", etName.getText().toString());
                                    map.put("detail", list.get(pos).get("detail"));
                                    list.remove(list.get(pos));
                                    list.add(pos, map);
                                    adapter.notifyDataSetChanged();
                                    SQLiteDatabase database = dbHelper.getWritableDatabase();
                                    ContentValues values = new ContentValues();
                                    values.put("NAME", etName.getText().toString());
                                    database.update(SQLiteDBHelper.TABLE_NAME, values, "ID=?", new String[]{map.get("id")});
                                    database.close();
                                }
                            })
                            .show();
                }
            }
        });
        recyclerView.setAdapter(adapter);
        textView.setText(String.format(Locale.getDefault(), "共%d条记录", cursor.getCount()));
    }

    private void initSmoothMove() {
        shipPointList = new ArrayList<>();
        shipPointList.add(new LatLng(0, 0));
        smoothMoveMarker = new SmoothMoveMarker(aMap);
        smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.ship));
    }

    private void morph(int state) {
        if (state != NONE) {
            this.state = state;
        }
        switch (state) {
            case UNREADY:
                Toast.makeText(this, "连接中断，请重新连接", Toast.LENGTH_SHORT).show();
                btnConnect.setVisibility(View.VISIBLE);
                ll_mark.setVisibility(View.GONE);
                ll_method.setVisibility(View.GONE);
                ll_nav.setVisibility(View.GONE);
                ll_finish.setVisibility(View.GONE);
                btnHome.setVisibility(View.GONE);
                tvToolbar.setText(getResources().getString(R.string.app_name));
                resetMap();
                break;
            case READY:
                btnConnect.setVisibility(View.GONE);
                ll_nav.setVisibility(View.GONE);
                ll_finish.setVisibility(View.GONE);
                ll_mark.setVisibility(View.VISIBLE);
                ll_method.setVisibility(View.VISIBLE);
                btnHome.setVisibility(View.VISIBLE);
                tvToolbar.setText(getResources().getString(R.string.app_name));
                break;
            case GONE:
                btnGostop.setText("暂停");
                btnGostop.setCompoundDrawables(null, picPause, null, null);
                ll_mark.setVisibility(View.GONE);
                ll_method.setVisibility(View.INVISIBLE);
                ll_nav.setVisibility(View.VISIBLE);
                btnHome.setVisibility(View.GONE);
                tvToolbar.setText(sw_nav.getSelectedTab() == 0 ? "正处于单次自主导航" : "正处于循环自主导航");
                break;
            case PAUSE:
                btnGostop.setText("开始");
                btnGostop.setCompoundDrawables(null, picStart, null, null);
                break;
        }
    }

    private void showLoadingView() {
        if (loadingView.getVisibility() == View.GONE) {
            state = UNREADY;
            findViewById(R.id.loadingview).setVisibility(View.VISIBLE);
            AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(300);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            findViewById(R.id.loadingview).startAnimation(animation);
        }

    }

    @Override
    public void onConnected() {
//        mHandler.sendMessage(mHandler.obtainMessage(8, READY));
        new Thread(new QueryThread()).start();
        new Thread(new QueryStateTread()).start();
    }

    private void checkUpdate() {
        System.out.println("check update");
        new UpdateWrapper.Builder(getApplicationContext())
                .setTime(1000)
                .setNotificationIcon(R.mipmap.ic_launcher)
                .setUrl("http://rty813.xyz/cleanship/app.json")
                .setIsShowToast(false)
                .setCallback(new CheckUpdateTask.Callback() {
                    @Override
                    public void callBack(VersionModel versionModel) {
                        Log.d("Update", "new version :" + versionModel.getVersionCode() + ";version info" + versionModel.getVersionName());
                    }
                })
                .build()
                .start();
    }

    private void requestPermission() {
        AndPermission.with(this)
                .permission(Permission.ACCESS_COARSE_LOCATION, Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
                        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
                                .strokeColor(Color.parseColor("#00000000"))
                                .radiusFillColor(Color.parseColor("#00000000"));

                        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
                        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
                        checkUpdate();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        final SettingService settingService = AndPermission.permissionSetting(NewActivity.this);
                        new AlertDialog.Builder(NewActivity.this)
                                .setMessage("请赋予权限")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        settingService.execute();
                                        finish();
                                    }
                                })
                                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        settingService.cancel();
                                        finish();
                                    }
                                })
                                .show();
                    }
                })
                .rationale(new Rationale() {
                    @Override
                    public void showRationale(Context context, List<String> permissions, final RequestExecutor executor) {
                        final PopupWindow popupWindow = new PopupWindow();
                        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                        View contentView = LayoutInflater.from(NewActivity.this).inflate(R.layout.permission_ask, null);
                        contentView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                popupWindow.dismiss();
                                executor.execute();
                            }
                        });
                        popupWindow.setContentView(contentView);
                        popupWindow.setAnimationStyle(R.style.dismiss_anim);
                        btnConnect.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!NewActivity.this.isFinishing()) {
                                    popupWindow.showAtLocation(NewActivity.this.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                                }
                            }
                        });
                    }
                })
                .start();
    }

    private void saveRoute(String time, String route, String address) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("TIME", time);
        cv.put("ROUTE", route);
        cv.put("NAME", address);
        database.insert(SQLiteDBHelper.TABLE_NAME, null, cv);
        database.close();
    }

    private void loadRoute(@Nullable String id) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        resetMap();
        Cursor cursor;
        if (id != null) {
            cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, "ID=?", new String[]{id}, null, null, null);
            cursor.moveToFirst();
        } else {
            cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, null, null, null, null, null);
            cursor.moveToLast();
        }
        if (cursor.getCount() > 0) {
            String route = cursor.getString(cursor.getColumnIndex("ROUTE"));
            String[] markers_str = route.split(";");
            for (int i = 0; i < markers_str.length; i++) {
                String[] location = markers_str[i].split(",");
                LatLng latLng = new LatLng(Double.parseDouble(location[0]), Double.parseDouble(location[1]));
                if (i == 0) {
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

    private void resetMap() {
        aMap.clear(true);
        shipPointList.removeAll(shipPointList);
        shipPointList.add(new LatLng(0, 0));
        aimPointList.removeAll(aimPointList);
        markers.removeAll(markers);
        polylines.removeAll(polylines);
        trace.removeAll(trace);
        initSmoothMove();
    }

    static class MyHandler extends Handler {
        WeakReference<NewActivity> mActivity;

        MyHandler(NewActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final NewActivity activity = mActivity.get();
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
            switch (msg.what) {
                case 1:
                    WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
                    lp.alpha = (float) msg.obj;
                    activity.getWindow().setAttributes(lp);
                    break;
                case 2:
                    ((PopupWindow) msg.obj).showAtLocation(activity.mMapView, Gravity.CENTER, 0, 0);
                    break;
                case 3:
                    if (activity.loadingView.getVisibility() == View.VISIBLE) {
                        activity.loadingView.startAnimation(animation);
                    }
                    activity.morph(UNREADY);
                    break;
                case 4:
                    break;
                case 5:
                    Log.d("非法数据", (String) msg.obj);
                    break;
                case 6:
                    if (activity.loadingView.getVisibility() == View.VISIBLE) {
                        activity.loadingView.startAnimation(animation);
                    }
                    break;
                case 7:
//                    btn_start.setEnabled(true);
                    break;
                case 8:
                    if (activity.loadingView.getVisibility() == View.VISIBLE) {
                        activity.loadingView.startAnimation(animation);
                    }
                    synchronized (activity.serialPort) {
                        activity.morph((Integer) msg.obj);
                        activity.serialPort.notify();
                    }
                    break;
                case 9:
                    Toast.makeText(activity, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class QueryThread implements Runnable {
        @Override
        public void run() {
            synchronized (serialPort) {
                int type = 0;
                boolean err = false;
                long retry_times = 0;
                while (true) {
                    String data = null;
                    try {
                        if (state == UNREADY) {
                            serialPort.wait();
                        }
                        if (!err) {
                            serialPort.writeData(String.format(Locale.getDefault(), "$QUERY,%d#", type), 10);
                        }
                        StringBuilder builder = new StringBuilder();
                        while (!(data = serialPort.readData()).equals("")) {
                            builder.append(data);
                            if (data.endsWith("#")) {
                                break;
                            }
                        }
                        Thread.sleep(1000);
                        data = builder.toString();
                        if (!data.startsWith("$") || !data.endsWith("#")) {
                            err = false;
                            Thread.sleep(1000);
                            continue;
                        } else {
                            data = data.replaceAll("#", "");
                            data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                        }

                        if (!data.equals("")) {
                            retry_times = 0;
                            String[] strings = data.split(";");
                            Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                            if (strings.length == 2) {
                                intent.putExtra("rawData", String.format(Locale.getDefault(),
                                        "|%d|%s|%s", type, strings[0], strings[1]));
                            } else {
                                intent.putExtra("rawData", data);
                            }
                            if (strings.length == 2 && Integer.parseInt(strings[0]) < 6) {
                                intent.putExtra("type", Integer.parseInt(strings[0]));
                                intent.putExtra("data", strings[1]);
                            }
                            if (strings.length == 2 && Integer.parseInt(strings[0]) == type) {
                                type = (type + 1) % 6;
                                err = false;
                            } else if (strings.length == 2) {
                                err = true;
                            }
                            sendBroadcast(intent);
                        } else {
                            err = false;
                            retry_times++;
                            if (retry_times % 5 == 0) {
                                mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                            }
                        }
                    } catch (NumberFormatException | InterruptedException e) {
                        mHandler.sendMessage(mHandler.obtainMessage(5, data));
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                        mHandler.sendEmptyMessage(3);
                    }
                }
            }
        }
    }

    private class QueryStateTread implements Runnable {

        @Override
        public void run() {
            int retry_times = 0;
            try {
                do {
                    serialPort.writeData("$QUERY,7#", 10);
                    String data;
                    StringBuilder builder = new StringBuilder();
                    while (!(data = serialPort.readData()).equals("")) {
                        builder.append(data);
                        if (data.endsWith("#")) {
                            break;
                        }
                    }
                    Thread.sleep(1000);
                    data = builder.toString();
                    if (!data.startsWith("$") || !data.endsWith("#")) {
                        Thread.sleep(1000);
                        continue;
                    } else {
                        data = data.replaceAll("#", "");
                        data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                    }
                    if (!data.equals("")) {
                        String[] strings = data.split(";");
                        if (strings.length == 2 && Integer.parseInt(strings[0]) == 7) {
                            int state = Integer.parseInt(strings[1]);
                            if (state == 0){
                                mHandler.sendMessage(mHandler.obtainMessage(8, READY));
                            }
                            else {
                                sw_nav.setSelectedTab(state > 0 ? 1 : 0);
                                if (state > 0){
                                    tvCircle.setText(String.format(Locale.getDefault(), "第%d圈", state));
                                }
                                mHandler.sendMessage(mHandler.obtainMessage(8, GONE));
                            }
                            break;
                        }
                    }
                    retry_times++;
                    if (retry_times == 2) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                    }
                    if (retry_times == 5) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "发送失败"));
                        mHandler.sendMessage(mHandler.obtainMessage(8, READY));
                        break;
                    }
                } while (true);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } finally {
                mHandler.sendEmptyMessage(7);
            }
        }
    }

    private class WriteSerialThread implements Runnable {
        private final String mData;
        private final int mState;
        private final int mPreState;

//        WriteSerialThread(NewActivity activity, String data, int state) {
//            mData = data;
//            mState = state;
//            mPreState = activity.state;
//            showLoadingView();
//        }

        WriteSerialThread(String data, int state, int preState){
            mData = data;
            mState = state;
            mPreState = preState;
        }

        @Override
        public void run() {
            int retry_times = 0;
            try {
                do {
                    serialPort.writeData(mData, 10);
                    String data;
                    StringBuilder builder = new StringBuilder();
                    while (!(data = serialPort.readData()).equals("")) {
                        builder.append(data);
                        if (data.endsWith("#")) {
                            break;
                        }
                    }
                    Thread.sleep(1000);
                    data = builder.toString();
                    Log.i("writeSerialPort", data);
                    if (data.contains(mData)) {
                        mHandler.sendMessage(mHandler.obtainMessage(8, mState));
                        break;
                    }
                    retry_times++;
                    if (retry_times == 2) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                    }
                    if (retry_times == 4) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "发送失败"));
                        mHandler.sendMessage(mHandler.obtainMessage(8, mPreState));
                        break;
                    }
                } while (true);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } finally {
                mHandler.sendEmptyMessage(7);
            }
        }
    }
    public ArrayList<LatLng> getShipPointList() {
        return shipPointList;
    }
    public void move(){
        if (shipPointList.size() == 2){
            LatLngBounds bounds = new LatLngBounds(shipPointList.get(1), shipPointList.get(shipPointList.size() - 1));
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
        }
        else{
            List<LatLng> subList = shipPointList.subList(shipPointList.size() - 2, shipPointList.size());
            smoothMoveMarker.setPoints(subList);
            smoothMoveMarker.setTotalDuration(1);
            smoothMoveMarker.startSmoothMove();
            trace.add(aMap.addPolyline(new PolylineOptions().add(shipPointList.get(shipPointList.size() - 2),
                    shipPointList.get(shipPointList.size() - 1)).width(5).color(Color.parseColor("#FFE418"))));
        }
    }
}

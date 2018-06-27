package com.cn.orcatech.cleanship.mqtt;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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
import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.SwipeRecyclerViewAdapter;
import com.cn.orcatech.cleanship.activity.QRScanActivity;
import com.cn.orcatech.cleanship.util.SQLiteDBHelper;
import com.cn.orcatech.cleanship.util.WriteSerialThreadFactory;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.kcode.lib.UpdateWrapper;
import com.kcode.lib.bean.VersionModel;
import com.kcode.lib.net.CheckUpdateTask;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
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
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import es.dmoral.toasty.Toasty;
import lib.kingja.switchbutton.SwitchMultiButton;

import static com.cn.orcatech.cleanship.activity.QRScanActivity.DEVICE_INFO;


public class MqttActivity extends AppCompatActivity implements View.OnClickListener {

    private MqttService mqttService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mqttService = ((MqttService.MyBinder) iBinder).getService();
            mqttService.startBackgroundThread(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("err", "disconnected");
        }
    };

    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int GONE = 2;
    private static final int NONE = -1;
    private static final int HOMING = 5;
    private static final int FINISH = 6;
    private static final int PAUSE = 4;
    private static final String MY_APPID = "2882303761517784606";
    private static final String MY_APP_KEY = "5451778422606";
    private static final String CHANNEL = "SELF";
    private static final double CTL_RADIUS = 2000;
    public static SQLiteDBHelper dbHelper;
    public MyHandler mHandler;
    private AMap aMap;
    private MapView mMapView;
    private long mExitTime;
    private int state;
    private boolean markEnable = false;
    private String pos = null;
    private ProgressDialog loadingView;
    private MyReceiver myReceiver;
    private ArrayList<LatLng> shipPointList;
    private ArrayList<LatLng> aimPointList;
    private SmoothMoveMarker smoothMoveMarker;
    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;
    private ArrayList<Polyline> trace;
    private Button btnConnect;
    private LinearLayout llFinish;
    private LinearLayout llNav;
    private LinearLayout llMark;
    private LinearLayout llMethod;
    private LinearLayout llHome;
    private SwitchMultiButton swNav;
    private Button btnHome;
    private Button btnHome2;
    private Button btnGoStop;
    private AppCompatSeekBar seekBar;
    private Button btnVel;
    private Button btnHistory;
    private Button btnManual;
    private Button btnAbort;
    private Drawable picPause;
    private Drawable picStart;
    private Drawable picMarkEnable;
    private Drawable picMarkDisable;
    private FloatingActionButton btnCtl;
    private Button btnEnable;
    private TextView tvFinish;
    private TextView tvShipCharge;
    private TextView tvToolbar;
    private TextView tvDate;
    private TextView tvCircle;
    private Circle limitCircle;
    private long routeID = -1;
    private int preState = Integer.MAX_VALUE;
    private ExecutorService writeSerialThreadPool;
    private FloatingActionMenu fam;
    private String toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        MiStatInterface.initialize(this, MY_APPID, MY_APP_KEY, CHANNEL);
        MiStatInterface.setUploadPolicy(MiStatInterface.UPLOAD_POLICY_REALTIME, 0);
        MiStatInterface.enableExceptionCatcher(true);
        URLStatsRecorder.enableAutoRecord();
        bindService(new Intent(MqttActivity.this, MqttService.class), serviceConnection, BIND_AUTO_CREATE);
        startService(new Intent(MqttActivity.this, MqttService.class));
        initView(savedInstanceState);
        initAMap();
        initClass();
        requestPermission();
        btnCtl.postDelayed(new Runnable() {
            @Override
            public void run() {
                setWhiteList();
            }
        }, 500);
    }

    @Override
    protected void onResume() {
        MiStatInterface.recordPageStart(MqttActivity.this, "主界面");
        mMapView.onResume();
        myReceiver = new MyReceiver(null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MqttService.MQTT_ONCONNCET);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(MyReceiver.ACTION_DATA_RECEIVED);
        registerReceiver(myReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        MiStatInterface.recordPageEnd();
        mMapView.onPause();
        unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (!mqttService.isConnected) {
            stopService(new Intent(this, MqttService.class));
        } else {
            mqttService.startBackgroundThread(true);
        }
        state = UNREADY;
        unbindService(serviceConnection);
        mMapView.onDestroy();
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
                Toasty.info(this, "再按一次退出", Toast.LENGTH_SHORT).show();
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
        loadingView = new ProgressDialog(this);
        loadingView.setMessage("发送中");
        loadingView.setTitle("提示");
        loadingView.setCanceledOnTouchOutside(false);
        loadingView.setCancelable(false);
        loadingView.setIcon(R.mipmap.ic_launcher);
        initSmoothMove();
        state = UNREADY;
        writeSerialThreadPool = new ThreadPoolExecutor(1, 1, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), new WriteSerialThreadFactory());
    }

    private void initView(Bundle savedInstanceState) {
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);

        btnConnect = findViewById(R.id.btn_connect);
        btnHome = findViewById(R.id.btn_home);
        llNav = findViewById(R.id.ll_nav);
        llMark = findViewById(R.id.ll_mark);
        llMethod = findViewById(R.id.ll_method);
        llFinish = findViewById(R.id.ll_finish);
        llHome = findViewById(R.id.ll_home);
        swNav = findViewById(R.id.sw_nav);
        tvToolbar = findViewById(R.id.tv_toolbar);
        tvCircle = findViewById(R.id.tv_circle);
        tvFinish = findViewById(R.id.tv_finish);
        tvDate = findViewById(R.id.tv_date);
        btnGoStop = findViewById(R.id.btn_gostop);
        btnHome2 = findViewById(R.id.btn_home2);
        btnHistory = findViewById(R.id.btn_history);
        btnManual = findViewById(R.id.btn_manual);
        btnVel = findViewById(R.id.btn_vel);
        btnAbort = findViewById(R.id.btn_abort);
        btnEnable = findViewById(R.id.btn_enable);
        btnCtl = findViewById(R.id.btn_ctl);
        seekBar = findViewById(R.id.seekbar);
        fam = findViewById(R.id.fam);
        tvShipCharge = findViewById(R.id.tv_shipcharge);
        final SharedPreferences sharedPreferences = this.getSharedPreferences("cleanship", MODE_PRIVATE);
        seekBar.setProgress(sharedPreferences.getInt("seekbar", 450));
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
                btnGoStop.setVisibility(View.VISIBLE);
                btnAbort.setVisibility(View.VISIBLE);
                seekBar.setVisibility(View.GONE);
                sharedPreferences.edit().putInt("seekbar", seekBar.getProgress()).apply();
                writeSerialThreadPool.execute(new WriteSerialThread(String.format(Locale.getDefault(),
                        "$ORDER,6,%d#", 1400 + seekBar.getProgress()), GONE, state));
            }
        });

        findViewById(R.id.btn_go).setOnClickListener(this);
        findViewById(R.id.btn_enable).setOnClickListener(this);
        findViewById(R.id.btn_delete).setOnClickListener(this);
        findViewById(R.id.btn_vel).setOnClickListener(this);
        findViewById(R.id.btn_home2).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_reload).setOnClickListener(this);
        findViewById(R.id.btn_finish).setOnClickListener(this);
        findViewById(R.id.btn_stop_home).setOnClickListener(this);
        btnCtl.setOnClickListener(this);
        btnAbort.setOnClickListener(this);
        btnManual.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
        btnHome.setOnClickListener(this);
        btnGoStop.setOnClickListener(this);
        btnEnable.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        findViewById(R.id.btn_changemap).setOnClickListener(this);
        findViewById(R.id.btn_bind).setOnClickListener(this);

        picStart = getResources().getDrawable(R.drawable.btn_start_selector);
        picPause = getResources().getDrawable(R.drawable.btn_pause_selector);
        picMarkEnable = getResources().getDrawable(R.drawable.mark_y);
        picMarkDisable = getResources().getDrawable(R.drawable.mark_n);
        picStart.setBounds(0, 0, picStart.getMinimumWidth(), picStart.getMinimumHeight());
        picPause.setBounds(0, 0, picPause.getMinimumWidth(), picPause.getMinimumHeight());
        picMarkDisable.setBounds(0, 0, picMarkDisable.getMinimumWidth(), picMarkDisable.getMinimumHeight());
        picMarkEnable.setBounds(0, 0, picMarkEnable.getMinimumWidth(), picMarkEnable.getMinimumHeight());

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        };
        llFinish.setOnTouchListener(onTouchListener);
        llHome.setOnTouchListener(onTouchListener);
        llMark.setOnTouchListener(onTouchListener);
        llMethod.setOnTouchListener(onTouchListener);
        llNav.setOnTouchListener(onTouchListener);
        toolbarTitle = getSharedPreferences(DEVICE_INFO, MODE_PRIVATE).getString("name", "") + " " + getResources().getString(R.string.app_name);
        tvToolbar.setText(toolbarTitle);
    }

    private void initAMap() {
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        aMap.getUiSettings().setCompassEnabled(false);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                if ((markEnable) && (markers.size() > 1) && ("1".equals(marker.getTitle())) && (!"1".equals(markers.get(markers.size() - 1).getTitle()))) {
                    marker.hideInfoWindow();
                    LatLng latLng = markers.get(markers.size() - 1).getPosition();
                    MarkerOptions markerOptions = new MarkerOptions().position(marker.getPosition());
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                    markerOptions.anchor(0.5f, 0.5f);
                    markerOptions.setFlat(true);
                    markerOptions.draggable(true);
                    markers.add(aMap.addMarker(markerOptions));
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng, marker.getPosition()).width(14).color(Color.parseColor("#0B76CE"))));
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
                    PolylineOptions options = new PolylineOptions().width(10).color(Color.parseColor("#0B76CE"));
                    if (index == 0) {
                        options.add(latLng, markers.get(1).getPosition());
                        polylines.get(0).setOptions(options);
                    } else if (index == markers.size() - 1) {
                        options.add(markers.get(markers.size() - 2).getPosition(), latLng);
                        polylines.get(polylines.size() - 1).setOptions(options);
                    } else {
                        options.add(markers.get(index - 1).getPosition(), latLng);
                        polylines.get(index - 1).setOptions(options);
                        options = new PolylineOptions().width(10).color(Color.parseColor("#0B76CE")).add(latLng, markers.get(index + 1).getPosition());
                        polylines.get(index).setOptions(options);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
            }
        });
        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (!markEnable) {
                    return;
                }
                if (AMapUtils.calculateLineDistance(latLng, limitCircle.getCenter()) > CTL_RADIUS) {
                    Toasty.warning(MqttActivity.this, "超出遥控范围", Toast.LENGTH_SHORT).show();
                }

                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
                markerOptions.title(String.valueOf(markers.size() + 1));
                markerOptions.snippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                markerOptions.anchor(0.5f, 0.5f);
                markerOptions.setFlat(true);
                markerOptions.draggable(true);

                System.out.println(latLng.toString());
                if (markers.size() > 0) {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                    LatLng lastLatlng = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng, lastLatlng).width(14)
                            .color(Color.parseColor("#0B76CE"))));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao_start)));
                    GeocodeSearch geocodeSearch = new GeocodeSearch(MqttActivity.this);
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
//                                System.out.println(pos);
                            }
                        }

                        @Override
                        public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

                        }
                    });
                    RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 1000, GeocodeSearch.AMAP);
                    geocodeSearch.getFromLocationAsyn(query);
                }
                markers.add(aMap.addMarker(markerOptions));
            }
        });
        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (limitCircle != null){
                    limitCircle.remove();
                }
                limitCircle = aMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(), location.getLongitude()))
                        .radius(CTL_RADIUS).strokeColor(Color.RED).strokeWidth(8).fillColor(Color.argb(20, 1, 1, 1)));
            }
        });
    }

    @Override
    public void onClick(View view) {
        PopupWindow popupHistory;
        switch (view.getId()) {
            case R.id.btn_connect:
                if (!loadingView.isShowing()) {
                    mqttService.setDeviceID(getSharedPreferences(DEVICE_INFO, MODE_PRIVATE).getString("id", null), 0);
                    if (mqttService.DEVICE_ID == null) {
                        Toasty.info(MqttActivity.this, "请先扫码绑定数传电台", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MqttActivity.this, QRScanActivity.class));
                        return;
                    }
                    ConnectivityManager manager = (ConnectivityManager) MqttActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = manager != null ? manager.getActiveNetworkInfo() : null;
                    if (networkInfo == null || !networkInfo.isAvailable()) {
                        Toasty.error(MqttActivity.this, "请检查网络连接！", Toast.LENGTH_LONG).show();
                    }
                    else {
                        showLoadingView("正在连接");
                        mqttService.connect();
                    }
                }
                break;
            case R.id.btn_changemap:
                if (aMap.getMapType() == AMap.MAP_TYPE_NORMAL) {
                    aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                } else {
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                }
                fam.close(true);
                break;
            case R.id.btn_bind:
                startActivity(new Intent(MqttActivity.this, QRScanActivity.class));
                mqttService.close();
                finish();
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
                    writeSerialThreadPool.execute(new WriteSerialThread("$ORDER,2#", HOMING, state));
                }
                break;
            case R.id.btn_enable:
                markEnable = !markEnable;
                btnEnable.setCompoundDrawables(null, markEnable ? picMarkEnable : picMarkDisable, null, null);
                break;
            case R.id.btn_go:
                if (state == READY) {
                    if (markers.size() > 0) {
                        for (Polyline line : trace) {
                            line.remove();
                        }
                        trace.removeAll(trace);
                        StringBuilder stringBuilder = new StringBuilder();
                        for (Marker marker : markers) {
                            stringBuilder.append(String.format(Locale.getDefault(), "%.6f,%.6f;", marker.getPosition().latitude, marker.getPosition().longitude));
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
                        final String date = dateFormat.format(new Date(System.currentTimeMillis()));
                        saveRoute(date, stringBuilder.toString(), pos);
//                            使QueryThread进入Wait
                        loadingView = new ProgressDialog(this);
                        loadingView.setTitle("发送中");
                        loadingView.setCanceledOnTouchOutside(false);
                        loadingView.setCancelable(false);
                        loadingView.setMax(markers.size());
                        loadingView.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        loadingView.setIcon(R.mipmap.ic_launcher);
                        loadingView.show();
                        String data = swNav.getSelectedTab() == 0 ? "$NAV,1#" : "$NAV,2#";
                        new MyAsyncTask(this, new WriteSerialThread(data, GONE, READY), date).execute();
                    } else {
                        loadRoute(null);
                    }
                }
                break;
            case R.id.btn_vel:
                btnGoStop.setVisibility(View.GONE);
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
                loadHistory(recyclerView, popupHistory);
                contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                final int height = getResources().getDisplayMetrics().heightPixels / 2;
                if (contentView.getMeasuredHeight() > height) {
                    popupHistory.setHeight(height);
                } else {
                    popupHistory.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                popupHistory.showAsDropDown(findViewById(R.id.ll_method));
                break;
            case R.id.btn_abort:
                writeSerialThreadPool.execute(new WriteSerialThread("$CLEAR#", READY, state));
                break;
            case R.id.btn_gostop:
                if (state == PAUSE) {
                    writeSerialThreadPool.execute(new WriteSerialThread("$GO#", GONE, state));
                } else if (state == GONE) {
                    writeSerialThreadPool.execute(new WriteSerialThread("$STOP#", PAUSE, state));
                }
                break;
            case R.id.btn_stop_home:
                writeSerialThreadPool.execute(new WriteSerialThread("$CLEAR#", READY, state));
                break;
            case R.id.btn_finish:
                writeSerialThreadPool.execute(new WriteSerialThread("$CLEAR#", READY, state));
                resetMap();
                break;
            case R.id.btn_reload:
                writeSerialThreadPool.execute(new WriteSerialThread("$CLEAR#", READY, state));
//                    resetMap();
//                    long id = getSharedPreferences("cleanship", MODE_PRIVATE).getLong("route", -1);
//                    loadRoute(id == -1 ? null : String.valueOf(id));
                break;
            case R.id.btn_ctl:
                fam.close(true);
                writeSerialThreadPool.execute(new WriteSerialThread("$ORDER,7#", NONE, state));
                break;
            default:
                break;
        }
    }

    private void loadHistory(final SwipeMenuRecyclerView recyclerView, final PopupWindow popupHistory) {
        final ArrayList<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, null, null ,null, null, null);
        if (cursor.getCount() > 0){
            cursor.moveToLast();
            do{
                Map<String, String> map = new HashMap();
                map.put("detail", cursor.getString(cursor.getColumnIndex("TIME")));
                map.put("title", cursor.getString(cursor.getColumnIndex("NAME")));
                map.put("id", cursor.getString(cursor.getColumnIndex("ID")));
                map.put("top", cursor.getString(cursor.getColumnIndex("TOP")));
                if (!Boolean.parseBoolean(map.get("top"))) {
                    list.add(map);
                }
            } while(cursor.moveToPrevious());
            cursor.moveToFirst();
            do {
                Map<String, String> map = new HashMap();
                map.put("detail", cursor.getString(cursor.getColumnIndex("TIME")));
                map.put("title", cursor.getString(cursor.getColumnIndex("NAME")));
                map.put("id", cursor.getString(cursor.getColumnIndex("ID")));
                map.put("top", cursor.getString(cursor.getColumnIndex("TOP")));
                if (Boolean.parseBoolean(map.get("top"))) {
                    list.add(0, map);
                }
            } while (cursor.moveToNext());
        }
        database.close();
        final SwipeRecyclerViewAdapter adapter = new SwipeRecyclerViewAdapter(list);
        adapter.notifyDataSetChanged();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setSwipeItemClickListener(new SwipeItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                popupHistory.dismiss();
                routeID = Long.parseLong(list.get(position).get("id"));
                loadRoute(list.get(position).get("id"));
            }
        });
        recyclerView.setSwipeMenuCreator(new SwipeMenuCreator() {
            @Override
            public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
                DisplayMetrics metrics = MqttActivity.this.getResources().getDisplayMetrics();
                SwipeMenuItem deleteItem = new SwipeMenuItem(MqttActivity.this).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_delete).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                SwipeMenuItem renameItem = new SwipeMenuItem(MqttActivity.this).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_rename).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                swipeRightMenu.addMenuItem(renameItem);
                swipeRightMenu.addMenuItem(deleteItem);

                SwipeMenuItem topItem = new SwipeMenuItem(MqttActivity.this).setWidth((int) (metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_top).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                swipeLeftMenu.addMenuItem(topItem);
            }
        });
        recyclerView.setSwipeMenuItemClickListener(new SwipeMenuItemClickListener() {
            @Override
            public void onItemClick(SwipeMenuBridge menuBridge) {
                menuBridge.closeMenu();
                final int pos = menuBridge.getAdapterPosition();
//                System.out.println(menuBridge.getDirection());
                if (menuBridge.getDirection() > 0) {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", list.get(pos).get("id"));
                    map.put("title", list.get(pos).get("title"));
                    map.put("detail", list.get(pos).get("detail"));
                    map.put("top", String.valueOf(!Boolean.parseBoolean(list.get(pos).get("top"))));
                    list.remove(pos);
                    list.add(pos, map);
                    adapter.notifyItemChanged(pos);
                    SQLiteDatabase database = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("TOP", map.get("top"));
                    database.update(SQLiteDBHelper.TABLE_NAME, values, "ID=?", new String[]{map.get("id")});
                    database.close();
                } else {
                    if (menuBridge.getPosition() == 1) {
                        Map<String, String> map = list.get(pos);
                        String id = map.get("id");
                        list.remove(pos);
                        adapter.notifyItemRemoved(pos);
                        adapter.notifyItemRangeChanged(pos, list.size() - pos);
                        SQLiteDatabase database = dbHelper.getWritableDatabase();
                        database.delete(SQLiteDBHelper.TABLE_NAME, "ID=?", new String[]{id});
                        database.close();
                    } else {
                        final EditText etName = new EditText(MqttActivity.this);
                        etName.setHint(list.get(pos).get("title"));
                        new AlertDialog.Builder(MqttActivity.this)
                                .setTitle("重命名路线")
                                .setView(etName)
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Map<String, String> map = new HashMap<>();
                                        map.put("id", list.get(pos).get("id"));
                                        map.put("title", etName.getText().toString());
                                        map.put("detail", list.get(pos).get("detail"));
                                        map.put("top", list.get(pos).get("top"));
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
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void initSmoothMove() {
        shipPointList = new ArrayList<>();
        shipPointList.add(new LatLng(0, 0));
        smoothMoveMarker = new SmoothMoveMarker(aMap);
        smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromView(View.inflate(this, R.layout.ship, null)));
    }

    private void hideAll() {
        llMark.setVisibility(View.GONE);
        llMethod.setVisibility(View.INVISIBLE);
        llNav.setVisibility(View.GONE);
        llFinish.setVisibility(View.GONE);
        llHome.setVisibility(View.GONE);
        btnHome.setVisibility(View.GONE);
        btnConnect.setVisibility(View.GONE);
        tvShipCharge.setVisibility(View.VISIBLE);
    }

    private void morph(int state) {
        if (state != NONE) {
            preState = this.state;
            this.state = state;
            this.markEnable = false;
            btnEnable.setCompoundDrawables(null, picMarkDisable, null, null);
            tvToolbar.setText(toolbarTitle);
            hideAll();
        }
        switch (state) {
            case UNREADY:
                Toasty.error(this, "连接中断，请重新连接", Toast.LENGTH_SHORT).show();
                btnConnect.setVisibility(View.VISIBLE);
                preState = Integer.MAX_VALUE;
                mqttService.close();
                resetMap();
                tvShipCharge.setVisibility(View.INVISIBLE);
                fam.hideMenu(false);
                break;
            case READY:
                llMark.setVisibility(View.VISIBLE);
                llMethod.setVisibility(View.VISIBLE);
                btnHome.setVisibility(View.VISIBLE);
                fam.showMenu(true);
                break;
            case GONE:
                btnGoStop.setText("暂停");
                btnGoStop.setCompoundDrawables(null, picPause, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvToolbar.setText(swNav.getSelectedTab() == 0 ? "正处于单次自主导航" : "正处于循环自主导航");
                tvCircle.setVisibility(swNav.getSelectedTab() == 0 ? View.GONE : View.VISIBLE);
                fam.showMenu(true);
                break;
            case PAUSE:
                btnGoStop.setText("开始");
                btnGoStop.setCompoundDrawables(null, picStart, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvToolbar.setText(swNav.getSelectedTab() == 0 ? "正处于单次自主导航" : "正处于循环自主导航");
                tvCircle.setVisibility(swNav.getSelectedTab() == 0 ? View.GONE : View.VISIBLE);
                fam.showMenu(true);
                break;
            case HOMING:
                llHome.setVisibility(View.VISIBLE);
                fam.showMenu(true);
                break;
            case FINISH:
                llFinish.setVisibility(View.VISIBLE);
                writeSerialThreadPool.execute(new QueryTimeDisThread());
                fam.showMenu(true);
                break;
            default:
                break;
        }
    }

    private void showLoadingView(String msg) {
        if (!loadingView.isShowing()) {
            state = UNREADY;
            loadingView = new ProgressDialog(this);
            loadingView.setTitle("提示");
            loadingView.setCanceledOnTouchOutside(false);
            loadingView.setCancelable(false);
            loadingView.setIcon(R.mipmap.ic_launcher);
            loadingView.setMessage(msg);
            loadingView.show();
        }
    }

    public void setWhiteList() {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                // 是否是省电模式
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                Class<?> cls = pm != null ? pm.getClass() : null;
                if (cls != null) {
                    Method method1 = cls.getMethod("isPowerSaveMode");
                    Method method2 = cls.getMethod("isIgnoringBatteryOptimizations", String.class);
                    boolean isSavedMode = (Boolean) method1.invoke(pm, new Object[]{});
                    boolean isWhileList = (Boolean) method2.invoke(pm, getPackageName());
                    Log.i("MqttService", "isSavedMode : " + isSavedMode + ", isWhileList :" + isWhileList);
                    if (!isWhileList) {
                        // 弹窗是否把当前App添加到白名单
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toasty.warning(MqttActivity.this, "为保证本应用在后台能正常运行，请允许将本应用加入到电池优化白名单", Toast.LENGTH_LONG).show();
                            }
                        });
                        try {
                            Intent ignore = new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                            ignore.setData(Uri.parse("package:" + getPackageName()));
                            ignore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(ignore);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onConnected() {
        mqttService.isConnected = true;
        new Thread(new QueryThread()).start();
        new Thread(new QueryStateTread()).start();
    }
    
    private void checkUpdate() {
        System.out.println("check update");
        new UpdateWrapper.Builder(getApplicationContext())
                .setTime(1000)
                .setNotificationIcon(R.mipmap.ic_launcher)
                .setUrl("http://orca-tech.cn/app/master/apk.json")
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
                .permission(Permission.ACCESS_COARSE_LOCATION, Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_PHONE_STATE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        MyLocationStyle myLocationStyle = new MyLocationStyle();
                        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
                                .strokeColor(Color.parseColor("#00000000"))
                                .radiusFillColor(Color.parseColor("#00000000"));
                        //设置定位蓝点的Style
                        aMap.setMyLocationStyle(myLocationStyle);
                        // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
                        aMap.setMyLocationEnabled(true);
                        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(14);
                        aMap.moveCamera(cameraUpdate);
                        checkUpdate();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        final SettingService settingService = AndPermission.permissionSetting(MqttActivity.this);
                        new AlertDialog.Builder(MqttActivity.this)
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
                        View contentView = LayoutInflater.from(MqttActivity.this).inflate(R.layout.permission_ask, null);
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
                                if (!MqttActivity.this.isFinishing()) {
                                    popupWindow.showAtLocation(MqttActivity.this.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
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
        SharedPreferences.Editor editor = getSharedPreferences("cleanship", MODE_PRIVATE).edit();
        if (database.insert(SQLiteDBHelper.TABLE_NAME, null, cv) == -1) {
            editor.putLong("route", routeID);
        } else {
            editor.putLong("route", -1);
        }
        editor.apply();
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
                markerOptions.anchor(0.5f, 0.5f);
                markerOptions.draggable(true);
                markerOptions.setFlat(true);
                if (markers.size() > 0) {
                    LatLng lastLatlng = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng, lastLatlng).width(14).color(Color.parseColor("#0B76CE"))));
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao_start)));
                }
                markers.add(aMap.addMarker(markerOptions));
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

    public void setShipCharge(int shipCharge) {
        tvShipCharge.setText(String.format(Locale.getDefault(), "剩余电量：%d%%", shipCharge));
    }

    public ArrayList<LatLng> getShipPointList() {
        return shipPointList;
    }

    public void move() {
        if (shipPointList.size() == 2) {
            LatLngBounds bounds = new LatLngBounds(shipPointList.get(1), shipPointList.get(shipPointList.size() - 1));
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
            CameraUpdate mCameraUpdate = CameraUpdateFactory.zoomTo(15);
            aMap.moveCamera(mCameraUpdate);
        } else {
            List<LatLng> subList = shipPointList.subList(shipPointList.size() - 2, shipPointList.size());
            smoothMoveMarker.setPoints(subList);
            smoothMoveMarker.setTotalDuration(1);
            smoothMoveMarker.startSmoothMove();
            smoothMoveMarker.getMarker().setInfoWindowEnable(false);
            smoothMoveMarker.getMarker().setFlat(true);
            smoothMoveMarker.getMarker().setAnchor(0.5f, 0.5f);
            trace.add(aMap.addPolyline(new PolylineOptions().add(shipPointList.get(shipPointList.size() - 2),
                    shipPointList.get(shipPointList.size() - 1)).width(7).color(Color.parseColor("#FFE418"))));
        }
    }

    public void handleState(int state) {
        if (state != preState && state >= -5) {
            long id = getSharedPreferences("cleanship", MODE_PRIVATE).getLong("route", -1);
            int tempState = UNREADY;
            swNav.setSelectedTab(0);
            switch (state) {
                case 0:
                    tempState = READY;
                    break;
                case -1:
                    tempState = GONE;
                    break;
                case -2:
                    swNav.setSelectedTab(1);
                    tempState = PAUSE;
                    break;
                case -3:
                    tempState = PAUSE;
                    break;
                case -4:
                    tempState = FINISH;
                    break;
                case -5:
                    tempState = HOMING;
                    break;
                default:
                    break;
            }
            if (state > 0) {
                tvCircle.setText(String.format(Locale.getDefault(), "第%d圈", state));
                swNav.setSelectedTab(1);
                tempState = GONE;
            }
            if (tempState != this.state) {
                if (state != -5 && state != 0) {
                    loadRoute(id == -1 ? null : String.valueOf(id));
                }
                this.state = UNREADY;
                mHandler.sendMessage(mHandler.obtainMessage(8, tempState));
            }
        }
    }

    public static class MyHandler extends Handler {
        WeakReference<MqttActivity> mActivity;

        MyHandler(MqttActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MqttActivity activity = mActivity.get();
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
                    if (activity.loadingView.isShowing()) {
                        activity.loadingView.dismiss();
                    }
                    activity.morph(UNREADY);
                    break;
                case 4:
                    break;
                case 5:
                    Log.d("非法数据", (String) msg.obj);
                    break;
                case 6:
                    if (activity.loadingView.isShowing()) {
                        activity.loadingView.dismiss();
                    }
                    break;
                case 7:
//                    btn_start.setEnabled(true);
                    break;
                case 8:
                    if (activity.loadingView.isShowing()) {
                        activity.loadingView.dismiss();
                    }
                    synchronized (activity.mqttService) {
                        activity.morph((Integer) msg.obj);
                        activity.mqttService.notify();
                    }
                    break;
                case 9:
                    Toasty.info(activity, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case 10:
                    activity.handleState((Integer) msg.obj);
                    break;
                case 11:
                    if (activity.loadingView.isShowing()) {
                        activity.loadingView.dismiss();
                    }
                    activity.tvFinish.setText(String.format(Locale.getDefault(),
                            "此次航行用时%d小时%d分，大约行走%d米", msg.arg1 / 60, msg.arg1 % 60, msg.arg2));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
                    activity.tvDate.setText(dateFormat.format(new Date(System.currentTimeMillis())));
                    synchronized (activity.mqttService) {
                        activity.state = FINISH;
                        activity.mqttService.notify();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private static class MyAsyncTask extends AsyncTask<Void, Integer, Void> {
        private String date;
        private WeakReference<MqttActivity> activity;
        private WriteSerialThread writeSerialThread;

        MyAsyncTask(MqttActivity activity, WriteSerialThread writeSerialThread, String date) {
            this.activity = new WeakReference<>(activity);
            this.writeSerialThread = writeSerialThread;
            this.date = date;
            activity.state = UNREADY;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MqttActivity activity = this.activity.get();
            ArrayList<Marker> markers = activity.markers;
            if (markers.size() > 1){
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    int i;
                    for (i = 0; i < markers.size() - 1; i++) {
                        Marker marker = markers.get(i);
                        stringBuilder.append(String.format(Locale.getDefault(), "[%.6f,%.6f],", marker.getPosition().longitude, marker.getPosition().latitude));
                    }
                    stringBuilder.append(String.format(Locale.getDefault(), "[%.6f,%.6f]", markers.get(i).getPosition().longitude, markers.get(i).getPosition().latitude));
                    byte[] data = ("latlng=" + stringBuilder.toString() + "&addr=" + activity.pos + "&date=" + date.replace(" ", "+")).getBytes();
                    URL url = new URL("http://orca-tech.cn/app/benenv/data_collect.php");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(5000);
                    connection.setRequestProperty("Content-Length", data.length + "");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("charset", "utf-8");
                    connection.setDoOutput(true);
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(data);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        Log.d("data_collect", "ok");
                    } else {
                        Log.d("data_collect", "err");
                    }
                    outputStream.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!activity.mqttService.isConnected) {
                return null;
            }
            activity.mqttService.writeData("$CLEAR#", 1000);
            for (int i = 0; i < markers.size(); i++) {
                double latitude = markers.get(i).getPosition().latitude;
                double longitude = markers.get(i).getPosition().longitude;
                if (!(activity.swNav.getSelectedTab() == 1 && i == markers.size() - 1
                        && latitude == markers.get(0).getPosition().latitude
                        && longitude == markers.get(0).getPosition().longitude)) {
                    if (!activity.mqttService.isConnected) {
                        return null;
                    }
                    activity.mqttService.writeData(String.format(Locale.getDefault(),
                            "$GNGGA,%.6f,%.6f#", latitude, longitude), 1000);
                    publishProgress(i + 1);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            activity.get().loadingView.setProgress(values[0]);
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (activity.get().mqttService.isConnected) {
                activity.get().writeSerialThreadPool.execute(writeSerialThread);
            }
            super.onPostExecute(aVoid);
        }
    }

    private class QueryThread implements Runnable {
        @Override
        public void run() {
            synchronized (mqttService) {
                int type = 0;
                long retryTimes = 0;
                while (!mqttService.notificationEnable) {
                    String data = null;
                    try {
                        if (state == UNREADY) {
                            mqttService.wait();
                        }
                        mqttService.writeData(String.format(Locale.getDefault(),
                                "$QUERY,%d#", (type == 0 ? 9 : (type % 5 == 0 ? 7 : 0))), 10);
                        data = mqttService.readData(false);
                        type = (type + 1) % 50;
                        for (int timeGap = 0; timeGap < 50 && !mqttService.notificationEnable && state != UNREADY; timeGap++) {
                            Thread.sleep(10);
                        }
                        if (mqttService.notificationEnable) {
                            break;
                        }
                        if (state == UNREADY) {
                            continue;
                        }
                        if (data.startsWith("$") && data.endsWith("#")) {
                            data = data.replaceAll("#", "");
                            data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                            if (!"".equals(data)) {
                                retryTimes = 0;
                                String[] strings = data.split(";");
                                Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                                if (strings.length == 2) {
                                    intent.putExtra("type", Integer.parseInt(strings[0]));
                                    intent.putExtra("data", strings[1]);
                                }
                                sendBroadcast(intent);
                            } else {
                                retryTimes++;
                                if (retryTimes % 5 == 0) {
                                    mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                                }
                            }
                        } else {
                            retryTimes++;
                            if (retryTimes % 5 == 0) {
                                mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                            }
                        }
                    } catch (NumberFormatException | InterruptedException e) {
                        mHandler.sendMessage(mHandler.obtainMessage(5, data));
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class QueryStateTread implements Runnable {

        QueryStateTread() {
            loadingView.setMessage("正在获取船状态");
        }

        @Override
        public void run() {
            int retryTimes = 0;
            try {
                do {
                    if (!mqttService.isConnected) {
                        mHandler.sendEmptyMessage(6);
                        break;
                    }
                    retryTimes++;
                    if (retryTimes == 3) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                    }
                    if (retryTimes == 6) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "发送失败"));
                        mHandler.sendMessage(mHandler.obtainMessage(8, READY));
                        break;
                    }
                    mqttService.writeData("$QUERY,7#", 10);
                    Thread.sleep(300);
                    String data = mqttService.readData(false);
                    if (!data.startsWith("$") || !data.endsWith("#")) {
                        continue;
                    } else {
                        data = data.replaceAll("#", "");
                        data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                    }
                    if (!"".equals(data)) {
                        String[] strings = data.split(";");
                        if (strings.length == 2 && Integer.parseInt(strings[0]) == 7) {
                            mHandler.sendMessage(mHandler.obtainMessage(10, Integer.parseInt(strings[1])));
                            break;
                        }
                    }
                } while (true);
            } catch (NumberFormatException | InterruptedException | ArrayIndexOutOfBoundsException e) {
                mHandler.sendMessage(mHandler.obtainMessage(9, "查询失败"));
                mHandler.sendMessage(mHandler.obtainMessage(8, READY));
                e.printStackTrace();
            } finally {
                mHandler.sendEmptyMessage(7);
            }
        }
    }

    private class WriteSerialThread implements Runnable {
        private final String mData;
        private final int mState;
        private final int mPreState;

        WriteSerialThread(String data, int state, int preState){
            mData = data;
            mState = state;
            mPreState = preState;
            showLoadingView("正在发送");
        }

        @Override
        public void run() {
            int retryTimes = 0;
            try {
                do {
                    if (!mqttService.isConnected) {
                        mHandler.sendEmptyMessage(6);
                        break;
                    }
                    mqttService.writeData(mData, 10);
                    Thread.sleep(1000);
                    String data = mqttService.readData(false);
                    Log.i("writeSerialPort", data);
                    if (data.contains(mData)) {
                        if (mState == NONE) {
                            state = mPreState;
                            mHandler.sendEmptyMessage(6);
                            synchronized (mqttService) {
                                mqttService.notify();
                            }
                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(8, mState));
                        }
                        break;
                    }
                    retryTimes++;
                    if (retryTimes == 2) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                    }
                    if (retryTimes == 4) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "发送失败"));
                        state = mPreState;
                        mHandler.sendEmptyMessage(6);
                        synchronized (mqttService) {
                            mqttService.notify();
                        }
                        break;
                    }
                } while (true);
            } catch (InterruptedException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } finally {
                mHandler.sendEmptyMessage(7);
            }
        }
    }

    private class QueryTimeDisThread implements Runnable {
        QueryTimeDisThread() {
            loadingView.dismiss();
            showLoadingView("正在查询航时航程");
        }

        @Override
        public void run() {
            int retryTimes = 0;
            try {
                do {
                    retryTimes++;
                    if (retryTimes == 3) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                    }
                    if (retryTimes == 6 || !mqttService.isConnected) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "发送失败"));
                        mHandler.sendMessage(mHandler.obtainMessage(11, 0, 0));
                        break;
                    }
                    mqttService.writeData("$QUERY,8#", 10);
                    Thread.sleep(300);
                    String data = mqttService.readData(false);
                    if (!data.startsWith("$") || !data.endsWith("#")) {
                        continue;
                    } else {
                        data = data.replaceAll("#", "");
                        data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                    }
                    if (!"".equals(data)) {
                        String[] strings = data.split(";");
                        if (strings.length == 2 && Integer.parseInt(strings[0]) == 8) {
                            strings = strings[1].split(",");
                            int time = (int) Math.round(Integer.parseInt(strings[0]) * 0.4 / 60);
                            int dis = Integer.parseInt(strings[1]);
                            mHandler.sendMessage(mHandler.obtainMessage(11, time, dis));
                            break;
                        }
                    }
                } while (true);
            } catch (NumberFormatException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

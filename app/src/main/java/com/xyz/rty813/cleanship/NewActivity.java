package com.xyz.rty813.cleanship;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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
import com.github.clans.fab.FloatingActionMenu;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.kcode.lib.UpdateWrapper;
import com.kcode.lib.bean.VersionModel;
import com.kcode.lib.net.CheckUpdateTask;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
import com.xyz.rty813.cleanship.ble.BluetoothLeService;
import com.xyz.rty813.cleanship.ble.CoreService;
import com.xyz.rty813.cleanship.util.SQLiteDBHelper;
import com.xyz.rty813.cleanship.util.SerialPortTool;
import com.xyz.rty813.cleanship.util.WriteSerialThreadFactory;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import app.dinus.com.loadingdrawable.LoadingView;
import es.dmoral.toasty.Toasty;
import lib.kingja.switchbutton.SwitchMultiButton;

/**
 * @author doufu
 */
public class NewActivity extends AppCompatActivity implements View.OnClickListener, SerialPortTool.onConnectedListener {
    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int GONE = 2;
    private static final int NAV = 3;
    private static final int NONE = -1;
    private static final int HOMING = 5;
    private static final int FINISH = 6;
    private static final int PAUSE = 4;
    private static final String MY_APPID = "2882303761517676503";
    private static final String MY_APP_KEY = "5131767662503";
    private static final String CHANNEL = "SELF";
    private static final int BAUD_RATE = 115200;
    private static final double CTL_RADIUS = 2000;
    public static SQLiteDBHelper dbHelper;
    public MyHandler mHandler;
    private AMap aMap;
    private MapView mMapView;
    private long mExitTime;
    private SerialPortTool serialPort;
    private int state;
    private boolean markEnable = false;
    private String pos = null;
    private LoadingView loadingView;
    private MyReceiver myReceiver;
    private BleStateReceiver bleStateReceiver;
    private ArrayList<LatLng> shipPointList;
    private ArrayList<LatLng> aimPointList;
    private SmoothMoveMarker smoothMoveMarker;
    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;
    private ArrayList<Polyline> trace;
    private float alpha = 1.0f;
    private Button btnConnect;
    private LinearLayout llFinish;
    private LinearLayout llNav;
    private LinearLayout llMark;
    private LinearLayout llMethod;
    private LinearLayout llHome;
    private SwitchMultiButton swNav;
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
    private FloatingActionButton btnCtl;
    private Button btnEnable;
    private TextView tvFinish;
    private TextView tvToolbar;
    private TextView tvCircle;
    private Circle limitCircle;
    private long routeID = -1;
    private int preState = Integer.MAX_VALUE;
    private ExecutorService writeSerialThreadPool;
    private CoreService coreService;
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_connect:
                    if (loadingView.getVisibility() == View.GONE) {
                        showLoadingView();
                        initBleSerial();
                    }
                    break;
                case R.id.fab_plane:
                    ((FloatingActionMenu) findViewById(R.id.fam)).close(true);
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                    break;
                case R.id.fab_satellite:
                    ((FloatingActionMenu) findViewById(R.id.fam)).close(true);
                    aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                    break;
                default:
                    break;
            }
        }
    };
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            coreService = ((CoreService.MyBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("err", "disconnected");
        }
    };

    private void initBleSerial() {
        showLoadingView();
        coreService.connect();
    }

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
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyReceiver.ACTION_DATA_RECEIVED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(myReceiver, filter);
        bleStateReceiver = new BleStateReceiver();
        filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        filter.addAction(BluetoothLeService.NPU_ACTION_GATT_DISCONNECTED);
        registerReceiver(bleStateReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        MiStatInterface.recordPageEnd();
        mMapView.onPause();
        unregisterReceiver(myReceiver);
        unregisterReceiver(bleStateReceiver);
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
        initSmoothMove();
        state = UNREADY;
        serialPort = new SerialPortTool(this);
        serialPort.registerReceiver(this);
        serialPort.setListener(this);
        writeSerialThreadPool = new ThreadPoolExecutor(1, 1, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), new WriteSerialThreadFactory());
    }

    private void initView(Bundle savedInstanceState) {
        loadingView = findViewById(R.id.loadingview);
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
        btnGostop = findViewById(R.id.btn_gostop);
        btnHome2 = findViewById(R.id.btn_home2);
        btnHistory = findViewById(R.id.btn_history);
        btnManual = findViewById(R.id.btn_manual);
        btnVel = findViewById(R.id.btn_vel);
        btnAbort = findViewById(R.id.btn_abort);
        btnEnable = findViewById(R.id.btn_enable);
        btnCtl = findViewById(R.id.btn_ctl);
        seekBar = findViewById(R.id.seekbar);
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
                btnGostop.setVisibility(View.VISIBLE);
                btnAbort.setVisibility(View.VISIBLE);
                seekBar.setVisibility(View.GONE);
                sharedPreferences.edit().putInt("seekbar", seekBar.getProgress()).apply();
                writeSerialThreadPool.execute(new WriteSerialThread(String.format(Locale.getDefault(),
                        "$ORDER,6,%d#", 1200 + seekBar.getProgress()), GONE, state));
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
        btnGostop.setOnClickListener(this);
        btnEnable.setOnClickListener(this);

        btnConnect.setOnClickListener(clickListener);
        findViewById(R.id.fab_plane).setOnClickListener(clickListener);
        findViewById(R.id.fab_satellite).setOnClickListener(clickListener);

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
                if ((markEnable) && (markers.size() > 1) && ("1".equals(marker.getTitle())) && (!"1".equals(markers.get(markers.size() - 1).getTitle()))) {
                    marker.hideInfoWindow();
                    LatLng latLng = markers.get(markers.size() - 1).getPosition();
                    MarkerOptions markerOptions = new MarkerOptions().position(marker.getPosition());
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                    markerOptions.anchor(0.5f, 0.5f);
                    markerOptions.setFlat(true);
                    markerOptions.draggable(true);
                    markers.add(aMap.addMarker(markerOptions));

                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng, marker.getPosition()).width(10).color(Color.parseColor("#0B76CE"))));
//                    Toast.makeText(NewActivity.this, "完成闭合回路！", Toast.LENGTH_SHORT).show();
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
            private int i;

            @Override
            public void onMapClick(LatLng latLng) {
                if (!markEnable) {
                    return;
                }
                if (AMapUtils.calculateLineDistance(latLng, limitCircle.getCenter()) > CTL_RADIUS) {
                    Toasty.warning(NewActivity.this, "超出遥控范围", Toast.LENGTH_SHORT).show();
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
                    LatLng latLng1 = latLng;
                    LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(10)
                            .color(Color.parseColor("#0B76CE"))));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao_start)));
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

    private void initSerialPort() {
        System.out.println("init serial port");
        List<UsbSerialDriver> list = serialPort.searchSerialPort();
        if (list.isEmpty()) {
            mHandler.sendEmptyMessage(6);
            Toasty.error(this, "未连接设备", Toast.LENGTH_SHORT).show();
        } else {
            try {
                serialPort.initDevice(list.get(0), BAUD_RATE);
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(6);
                Toasty.error(this, "连接失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (state != UNREADY) {
            PopupWindow popupHistory;
            switch (view.getId()) {
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
                                            if (!(swNav.getSelectedTab() == 1 && i == markers.size() - 1
                                                    && latitude == markers.get(0).getPosition().latitude
                                                    && longitude == markers.get(0).getPosition().longitude)) {
                                                serialPort.writeData(String.format(Locale.getDefault(),
                                                        "$GNGGA,%.6f,%.6f#", latitude, longitude), 500);
                                            }
                                        }
                                        String data = swNav.getSelectedTab() == 0 ? "$NAV,1#" : "$NAV,2#";
                                        writeSerialThreadPool.execute(new WriteSerialThread(data, GONE, READY));
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
                    break;
                case R.id.btn_reload:
                    writeSerialThreadPool.execute(new WriteSerialThread("$CLEAR#", READY, state));
                    resetMap();
                    long id = getSharedPreferences("cleanship", MODE_PRIVATE).getLong("route", -1);
                    loadRoute(id == -1 ? null : String.valueOf(id));
                    break;
                case R.id.btn_ctl:
                    writeSerialThreadPool.execute(new WriteSerialThread("$ORDER,7#", NONE, state));
                    break;
                default:
                    break;
            }
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
                DisplayMetrics metrics = NewActivity.this.getResources().getDisplayMetrics();
                SwipeMenuItem deleteItem = new SwipeMenuItem(NewActivity.this).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_delete).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                SwipeMenuItem renameItem = new SwipeMenuItem(NewActivity.this).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_rename).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                swipeRightMenu.addMenuItem(renameItem);
                swipeRightMenu.addMenuItem(deleteItem);

                SwipeMenuItem topItem = new SwipeMenuItem(NewActivity.this).setWidth((int) (metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_top).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                swipeLeftMenu.addMenuItem(topItem);
            }
        });
        recyclerView.setSwipeMenuItemClickListener(new SwipeMenuItemClickListener() {
            @Override
            public void onItemClick(SwipeMenuBridge menuBridge) {
                menuBridge.closeMenu();
                final int pos = menuBridge.getAdapterPosition();
                System.out.println(menuBridge.getDirection());
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
        smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.ship));
//        smoothMoveMarker.getMarker().setInfoWindowEnable(false);
    }

    private void hideAll() {
        llMark.setVisibility(View.GONE);
        llMethod.setVisibility(View.INVISIBLE);
        llNav.setVisibility(View.GONE);
        btnHome.setVisibility(View.GONE);
        llHome.setVisibility(View.GONE);
        btnConnect.setVisibility(View.GONE);
        llFinish.setVisibility(View.GONE);
        btnCtl.setVisibility(View.GONE);
    }

    private void morph(int state) {
        if (state != NONE) {
            this.state = state;
            this.markEnable = false;
            btnEnable.setCompoundDrawables(null, picMarkDisable, null, null);
            tvToolbar.setText(getResources().getString(R.string.app_name));
            hideAll();
        }
        switch (state) {
            case UNREADY:
                Toasty.error(this, "连接中断，请重新连接", Toast.LENGTH_SHORT).show();
                btnConnect.setVisibility(View.VISIBLE);
                preState = Integer.MAX_VALUE;
                resetMap();
                break;
            case READY:
                llMark.setVisibility(View.VISIBLE);
                llMethod.setVisibility(View.VISIBLE);
                btnHome.setVisibility(View.VISIBLE);
                btnCtl.setVisibility(View.VISIBLE);
                break;
            case GONE:
                btnGostop.setText("暂停");
                btnGostop.setCompoundDrawables(null, picPause, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvToolbar.setText(swNav.getSelectedTab() == 0 ? "正处于单次自主导航" : "正处于循环自主导航");
                tvCircle.setVisibility(swNav.getSelectedTab() == 0 ? View.GONE : View.VISIBLE);
                btnCtl.setVisibility(View.VISIBLE);
                break;
            case PAUSE:
                btnGostop.setText("开始");
                btnGostop.setCompoundDrawables(null, picStart, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvToolbar.setText(swNav.getSelectedTab() == 0 ? "正处于单次自主导航" : "正处于循环自主导航");
                tvCircle.setVisibility(swNav.getSelectedTab() == 0 ? View.GONE : View.VISIBLE);
                btnCtl.setVisibility(View.VISIBLE);
                break;
            case HOMING:
                llHome.setVisibility(View.VISIBLE);
                btnCtl.setVisibility(View.VISIBLE);
                break;
            case FINISH:
                writeSerialThreadPool.execute(new QueryTimeDisThread());
                break;
            default:
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
                        bindService(new Intent(NewActivity.this, CoreService.class), serviceConnection, BIND_AUTO_CREATE);
                        ConnectivityManager manager = (ConnectivityManager) NewActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
                        if (networkInfo == null || !networkInfo.isAvailable()) {
                            // 无网络
                            Toasty.error(NewActivity.this, "请检查网络连接！", Toast.LENGTH_LONG).show();
                        }

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
                        System.out.println(AndPermission.hasAlwaysDeniedPermission(NewActivity.this, permissions));
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
                    LatLng latLng1 = latLng;
                    LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(10).color(Color.parseColor("#0B76CE"))));
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
            trace.add(aMap.addPolyline(new PolylineOptions().add(shipPointList.get(shipPointList.size() - 2),
                    shipPointList.get(shipPointList.size() - 1)).width(5).color(Color.parseColor("#FFE418"))));
        }
    }

    public void handleState(int state) {
        if (state != preState) {
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
            preState = state;
        }
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
                    Toasty.info(activity, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case 10:
                    activity.handleState((Integer) msg.obj);
                    break;
                case 11:
                    if (activity.loadingView.getVisibility() == View.VISIBLE) {
                        activity.loadingView.startAnimation(animation);
                    }
                    activity.tvFinish.setText(String.format(Locale.getDefault(),
                            "此次航行用时%d小时%d分，大约行走%d米", msg.arg1 / 60, msg.arg1 % 60, msg.arg2));
                    activity.llFinish.setVisibility(View.VISIBLE);
                    activity.btnCtl.setVisibility(View.VISIBLE);
                    synchronized (activity.serialPort) {
                        activity.state = FINISH;
                        activity.serialPort.notify();
                    }
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
                long retryTimes = 0;
                while (true) {
                    String data = null;
                    try {
                        if (state == UNREADY) {
                            serialPort.wait();
                        }
                        serialPort.writeData(String.format(Locale.getDefault(), "$QUERY,%d#", type == 5 ? 7 : 0), 10);
                        StringBuilder builder = new StringBuilder();
                        while (!"".equals(data = serialPort.readData())) {
                            builder.append(data);
                            if (data.endsWith("#")) {
                                break;
                            }
                        }
                        Thread.sleep(300);
                        data = builder.toString();
                        if (!data.startsWith("$") || !data.endsWith("#")) {
                            continue;
                        } else {
                            data = data.replaceAll("#", "");
                            data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                        }

                        if (!"".equals(data)) {
                            retryTimes = 0;
                            String[] strings = data.split(";");
                            Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                            if (strings.length == 2) {
                                intent.putExtra("type", Integer.parseInt(strings[0]));
                                intent.putExtra("data", strings[1]);
                                type = (type + 1) % 6;
                            }
                            sendBroadcast(intent);
                        } else {
                            retryTimes++;
                            if (retryTimes % 5 == 0) {
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
            int retryTimes = 0;
            try {
                do {
                    retryTimes++;
                    if (retryTimes == 3) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "信号质量差"));
                    }
                    if (retryTimes == 6) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "发送失败"));
                        mHandler.sendMessage(mHandler.obtainMessage(8, READY));
//                        mHandler.sendMessage(mHandler.obtainMessage(10, 0));
                        break;
                    }
                    serialPort.writeData("$QUERY,7#", 10);
                    String data;
                    StringBuilder builder = new StringBuilder();
                    while (!"".equals(data = serialPort.readData())) {
                        builder.append(data);
                        if (data.endsWith("#")) {
                            break;
                        }
                    }
                    Thread.sleep(300);
                    data = builder.toString();
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
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } catch (NumberFormatException | InterruptedException e) {
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
            showLoadingView();
        }

        @Override
        public void run() {
            int retryTimes = 0;
            try {
                do {
                    serialPort.writeData(mData, 10);
                    String data;
                    StringBuilder builder = new StringBuilder();
                    while (!"".equals(data = serialPort.readData())) {
                        builder.append(data);
                        if (data.endsWith("#")) {
                            break;
                        }
                    }
                    Thread.sleep(1000);
                    data = builder.toString();
                    Log.i("writeSerialPort", data);
                    if (data.contains(mData)) {
                        if (mState == NONE) {
                            state = mPreState;
                            mHandler.sendEmptyMessage(6);
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

    private class QueryTimeDisThread implements Runnable {
        QueryTimeDisThread() {
            loadingView.setVisibility(View.GONE);
            showLoadingView();
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
                    if (retryTimes == 6) {
                        mHandler.sendMessage(mHandler.obtainMessage(9, "发送失败"));
                        mHandler.sendMessage(mHandler.obtainMessage(8, FINISH));
                        break;
                    }
                    serialPort.writeData("$QUERY,8#", 10);
                    String data;
                    StringBuilder builder = new StringBuilder();
                    while (!"".equals(data = serialPort.readData())) {
                        builder.append(data);
                        if (data.endsWith("#")) {
                            break;
                        }
                    }
                    Thread.sleep(300);
                    data = builder.toString();
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
                            int time = (int) (Integer.parseInt(strings[0]) * 0.4 / 60);
                            int dis = Integer.parseInt(strings[1]);
                            mHandler.sendMessage(mHandler.obtainMessage(11, time, dis));
                            break;
                        }
                    }
                } while (true);
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } catch (NumberFormatException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                mHandler.sendEmptyMessage(7);
            }
        }
    }
}

package com.cn.orcatech.cleanship.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.cn.orcatech.cleanship.MyReceiver;
import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.Ship;
import com.cn.orcatech.cleanship.ShiplistAdapter;
import com.cn.orcatech.cleanship.SwipeRecyclerViewAdapter;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.cn.orcatech.cleanship.mqtt.MqttService;
import com.cn.orcatech.cleanship.util.SQLiteDBHelper;
import com.cn.orcatech.cleanship.util.WriteThreadFactory;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.yanzhenjie.fragment.NoFragment;
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
import com.yanzhenjie.recyclerview.swipe.widget.DefaultItemDecoration;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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

import es.dmoral.toasty.Toasty;
import lib.kingja.switchbutton.SwitchMultiButton;

import static android.content.Context.MODE_PRIVATE;

public class MapFragment extends NoFragment implements View.OnClickListener {
    private static final String TAG = "MapFragment";
    public static SQLiteDBHelper dbHelper;
    private final float CTL_RADIUS = 2000;
    public MqttClient mqttClient;
    public MyHandler mHandler;
    private MapView mMapView;
    private AMap aMap;
    private boolean markEnable = false;
    private ArrayList<ArrayList<Marker>> markerLists;
    private ArrayList<ArrayList<Polyline>> polylineLists;
    private ArrayList<ArrayList<Polyline>> traceLists;
    private int[] colors;
    private Circle limitCircle;
    private String startPosition;
    private MyReceiver myReceiver;
    private Drawable picPause;
    private Drawable picStart;
    private Drawable picMarkEnable;
    private Drawable picMarkDisable;
    private Button btnPoweron;
    private LinearLayout llFinish;
    private LinearLayout llNav;
    private LinearLayout llMark;
    private LinearLayout llMethod;
    private LinearLayout llHome;
    private SwitchMultiButton swNav;
    private ArrayList<ArrayList<LatLng>> shipPointLists;
    private ArrayList<SmoothMoveMarker> smoothMoveMarkers;
    private ProgressDialog loadingView;
    private long routeID = -1;
    private String pos = null;
    private Button btnHome;
    private Button btnHome2;
    private Button btnGoStop;
    private AppCompatSeekBar seekBar;
    private Button btnVel;
    private Button btnHistory;
    private Button btnManual;
    private Button btnAbort;
    private FloatingActionButton btnCtl;
    private Button btnEnable;
    private TextView tvFinish;
    private TextView tvBattery;
    private TextView tvToolbar;
    private TextView tvDate;
    private TextView tvCircle;
    private ExecutorService mqttSendThreadPool;
    private FloatingActionMenu fam;
    private String toolbarTitle;
    private Toolbar toolbar;
    private MainActivity activity;
    public MqttCallback mqttCallBack = new MqttCallback() {
        @Override
        public void messageArrived(String topic, MqttMessage message) {
            if (message != null) {
                Log.d(TAG, "messageArrived, topic: " + topic + "; message: " + new String(message.getPayload()));
                if (topic.equals("test1")) {
                    DataFragment.setBtnText();
                    return;
                }
                if (message.toString().contains("ACK1")) {
                    mHandler.sendMessage(mHandler.obtainMessage(1, "发送成功"));
                    if (loadingView.isShowing()) {
                        loadingView.dismiss();
                    }
                    return;
                }
                else if (message.toString().contains("ACK0")) {
                    mHandler.sendMessage(mHandler.obtainMessage(2, "发送失败"));
                    if (loadingView.isShowing()) {
                        loadingView.dismiss();
                    }
                    return;
                }
                String[] topics = topic.split("/");
//                数据格式为 $state;battery;lat,lng#
                try {
                    int ship_id = Integer.parseInt(topics[2]);
                    if (ship_id > activity.userInfo.getTotalship() - 1) {
                        return;
                    }
                    String data = message.toString();
                    if (data.startsWith("$") && data.endsWith("#")) {
                        data = data.replaceAll("#", "");
                        data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                        if (!"".equals(data)) {
                            String[] strings = data.split(";");
                            if (strings.length < 7) {
                                return;
                            }
                            Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                            intent.putExtra("shipid", ship_id);
                            intent.putExtra("state", strings[0]);
                            intent.putExtra("latlng", strings[1]);
                            intent.putExtra("yaw", strings[2]);
                            intent.putExtra("pd_percent", strings[3]);
                            intent.putExtra("pd_rematime", strings[4]);
                            intent.putExtra("pd_current", strings[5]);
                            intent.putExtra("gps_speed", strings[6]);
                            intent.putExtra("gps_stars", strings[7]);

                            activity.sendBroadcast(intent);
                        }
                    }
//                    if (data.startsWith("#")) {
//                        status = "待机";
//                    }
//                    else if (data.startsWith("$")) {
//                        status = "在线";
//                    }
//                    else {
//                        status = "离线";
//                    }
//                    ships .get(ship_id).setStatus(status);
//                    if (shipPopupWindowList != null) {
//                        shipPopupWindowList.get(ship_id).put("status", status);
//                        activity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                shipPopupWindowAdapter.notifyItemChanged(ship_id);
//                            }
//                        });
//                    }
//                    if (ship_id != activity.selectShip) {
//                        return;
//                    }
                }
                catch (NumberFormatException e){
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "connectionLost");
            Toasty.error(activity, "连接中断！", Toast.LENGTH_SHORT).show();
            activity.logout();
        }
    };
    private ArrayList<Ship> ships;
    private ArrayList<Map<String, String>> shipPopupWindowList;
    private ShiplistAdapter shipPopupWindowAdapter;
    private String topicSend = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapView = view.findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        initView(view);
        initMap();
        initClass(0);
        initColor();
//        activity.bindService(new Intent(activity, MqttService.class), serviceConnection, BIND_AUTO_CREATE);
//        activity.startService(new Intent(activity, MqttService.class));
        requestPermission();
    }

    private void initColor() {
        colors = new int[10];
        colors[0] = 0xFFFF0000;
        colors[1] = 0xFFFFA200;
        colors[2] = 0xFFD0FF00;
        colors[3] = 0xFF37FF00;
        colors[4] = 0xFF00FF9D;
        colors[5] = 0xFF00D4FF;
        colors[6] = 0xFF0033FF;
        colors[7] = 0xFF6A00FF;
        colors[8] = 0xFFE100FF;
        colors[9] = 0xFFFF0080;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    private void initView(View view) {
        btnPoweron = view.findViewById(R.id.btn_poweron);
        btnHome = view.findViewById(R.id.btn_home);
        llNav = view.findViewById(R.id.ll_nav);
        llMark = view.findViewById(R.id.ll_mark);
        llFinish = view.findViewById(R.id.ll_finish);
        llHome = view.findViewById(R.id.ll_home);
        swNav = view.findViewById(R.id.sw_nav);
        tvToolbar = view.findViewById(R.id.tv_toolbar);
        tvCircle = view.findViewById(R.id.tv_circle);
        tvFinish = view.findViewById(R.id.tv_finish);
        tvDate = view.findViewById(R.id.tv_date);
        btnGoStop = view.findViewById(R.id.btn_gostop);
        btnHome2 = view.findViewById(R.id.btn_home2);
        btnHistory = view.findViewById(R.id.btn_history);
        btnManual = view.findViewById(R.id.btn_manual);
        btnVel = view.findViewById(R.id.btn_vel);
        btnAbort = view.findViewById(R.id.btn_abort);
        btnEnable = view.findViewById(R.id.btn_enable);
        btnCtl = view.findViewById(R.id.btn_ctl);
        seekBar = view.findViewById(R.id.seekbar);
        fam = view.findViewById(R.id.fam);
        tvBattery = view.findViewById(R.id.tv_shipcharge);
        toolbar = view.findViewById(R.id.toolbar);
        llMethod = view.findViewById(R.id.ll_method);
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("cleanship", MODE_PRIVATE);
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
                publishMessage(String.format(Locale.getDefault(), "$ORDER,6,%d#", 1400 + seekBar.getProgress()));
            }
        });

        view.findViewById(R.id.btn_go).setOnClickListener(this);
        view.findViewById(R.id.btn_enable).setOnClickListener(this);
        view.findViewById(R.id.btn_delete).setOnClickListener(this);
        view.findViewById(R.id.btn_vel).setOnClickListener(this);
        view.findViewById(R.id.btn_home2).setOnClickListener(this);
        view.findViewById(R.id.btn_cancel).setOnClickListener(this);
        view.findViewById(R.id.btn_reload).setOnClickListener(this);
        view.findViewById(R.id.btn_finish).setOnClickListener(this);
        view.findViewById(R.id.btn_stop_home).setOnClickListener(this);
        tvToolbar.setOnClickListener(this);
        btnCtl.setOnClickListener(this);
        btnAbort.setOnClickListener(this);
        btnManual.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
        btnHome.setOnClickListener(this);
        btnGoStop.setOnClickListener(this);
        btnEnable.setOnClickListener(this);
        btnPoweron.setOnClickListener(this);
        view.findViewById(R.id.btn_changemap).setOnClickListener(this);

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
        toolbarTitle = getResources().getString(R.string.app_name);
        tvToolbar.setText(toolbarTitle);
    }

    public void initClass(int totalship) {
        shipPointLists = new ArrayList<>(totalship);
        polylineLists = new ArrayList<>(totalship);
        traceLists = new ArrayList<>(totalship);
        markerLists = new ArrayList<>(totalship);
        smoothMoveMarkers = new ArrayList<>();
        ships = new ArrayList<>();
        for (int i = 0; i < totalship; i++) {
            ships.add(new Ship());
            shipPointLists.add(new ArrayList<LatLng>());
            polylineLists.add(new ArrayList<Polyline>());
            traceLists.add(new ArrayList<Polyline>());
            markerLists.add(new ArrayList<Marker>());
            shipPointLists.get(i).add(new LatLng(0, 0));
            smoothMoveMarkers.add(new SmoothMoveMarker(aMap));
            smoothMoveMarkers.get(i).setDescriptor(BitmapDescriptorFactory.fromView(View.inflate(activity, R.layout.ship, null)));
        }
        mHandler = new MyHandler(this);
        dbHelper = new SQLiteDBHelper(activity);
        loadingView = new ProgressDialog(activity);
        loadingView.setMessage("发送中");
        loadingView.setTitle("提示");
        loadingView.setCanceledOnTouchOutside(false);
        loadingView.setCancelable(false);
        loadingView.setIcon(R.mipmap.ic_launcher);
        mqttSendThreadPool = new ThreadPoolExecutor(1, 1, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), new WriteThreadFactory());
    }

    private void initMap() {
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
                if ((markEnable) && (markerLists.size() > 1) && ("1".equals(marker.getTitle())) && (!"1".equals(markerLists.get(activity.selectShip).get(markerLists.size() - 1).getTitle()))) {
                    marker.hideInfoWindow();
                    LatLng latLng = markerLists.get(activity.selectShip).get(markerLists.get(activity.selectShip).size() - 1).getPosition();
                    MarkerOptions markerOptions = new MarkerOptions().position(marker.getPosition());
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                    markerOptions.anchor(0.5f, 0.5f);
                    markerOptions.setFlat(true);
                    markerOptions.draggable(true);
                    markerLists.get(activity.selectShip).add(aMap.addMarker(markerOptions));
                    polylineLists.get(activity.selectShip).add(aMap.addPolyline(new PolylineOptions().add(latLng, marker.getPosition()).width(14).color(Color.parseColor("#0B76CE"))));
                }
                return true;
            }
        });
        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            private int index;

            @Override
            public void onMarkerDragStart(Marker marker) {
                for (index = 0; index < markerLists.size(); index++) {
                    if (markerLists.get(activity.selectShip).get(index).hashCode() == marker.hashCode()) {
                        break;
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                ArrayList<Marker> markers = markerLists.get(activity.selectShip);
                ArrayList<Polyline> polylines = polylineLists.get(activity.selectShip);
                LatLng latLng = marker.getPosition();
                marker.setSnippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                if (markerLists.size() > 1) {
                    PolylineOptions options = new PolylineOptions().width(10).color(Color.parseColor("#0B76CE"));
                    if (index == 0) {
                        options.add(latLng, markers.get(1).getPosition());
                        polylines.get(0).setOptions(options);
                    } else if (index == markerLists.size() - 1) {
                        options.add(markers.get(markerLists.size() - 2).getPosition(), latLng);
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

                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
                markerOptions.title(String.valueOf(markerLists.get(activity.selectShip).size() + 1));
                markerOptions.snippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                markerOptions.anchor(0.5f, 0.5f);
                markerOptions.setFlat(true);
                markerOptions.draggable(true);

                System.out.println(latLng.toString());
                if (markerLists.get(activity.selectShip).size() > 0) {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                    LatLng lastLatlng = markerLists.get(activity.selectShip).get(markerLists.get(activity.selectShip).size() - 1).getPosition();
                    polylineLists.get(activity.selectShip).add(aMap.addPolyline(new PolylineOptions().add(latLng, lastLatlng).width(14)
                            .color(Color.parseColor("#0B76CE"))));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao_start)));
                    GeocodeSearch geocodeSearch = new GeocodeSearch(activity);
                    geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
                        @Override
                        public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                            if (i == 1000) {
                                RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
                                startPosition = address.getProvince();
                                if (!address.getProvince().equals(address.getCity())) {
                                    startPosition += " " + address.getCity();
                                }
                                startPosition += " " + address.getDistrict();
                                if (address.getPois().size() != 0) {
                                    startPosition = startPosition + " " + address.getPois().get(0);
                                }
//                                System.out.println(startPosition);
                            }
                        }

                        @Override
                        public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

                        }
                    });
                    RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 1000, GeocodeSearch.AMAP);
                    geocodeSearch.getFromLocationAsyn(query);
                }
                markerLists.get(activity.selectShip).add(aMap.addMarker(markerOptions));
            }
        });
        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (limitCircle != null) {
                    limitCircle.remove();
                }
                limitCircle = aMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(), location.getLongitude()))
                        .radius(CTL_RADIUS).strokeColor(Color.RED).strokeWidth(8).fillColor(Color.argb(20, 1, 1, 1)));
            }
        });
    }

    private void showLoadingView(String msg) {
        if (!loadingView.isShowing()) {
            loadingView = new ProgressDialog(activity);
            loadingView.setTitle("提示");
            loadingView.setCanceledOnTouchOutside(false);
            loadingView.setCancelable(false);
            loadingView.setIcon(R.mipmap.ic_launcher);
            loadingView.setMessage(msg);
            loadingView.show();
        }
    }

    private void saveRoute(String time, String route, String address) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("TIME", time);
        cv.put("ROUTE", route);
        cv.put("NAME", address);
        SharedPreferences.Editor editor = activity.getSharedPreferences("cleanship", MODE_PRIVATE).edit();
        if (database.insert(SQLiteDBHelper.TABLE_NAME, null, cv) == -1) {
            editor.putLong("route", routeID);
        } else {
            editor.putLong("route", -1);
        }
        editor.apply();
        database.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        myReceiver = new MyReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MqttService.MQTT_ONCONNCET);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(MyReceiver.ACTION_DATA_RECEIVED);
        activity.registerReceiver(myReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
//        if (!mqttService.isConnected) {
//            activity.stopService(new Intent(activity, MqttService.class));
//        } else {
//            mqttService.startBackgroundThread(true);
//        }
//        state = UNREADY;
//        activity.unbindService(serviceConnection);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        activity.unregisterReceiver(myReceiver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

//    private void loadRoute(@Nullable String id) {
//        SQLiteDatabase database = dbHelper.getReadableDatabase();
//        resetMap();
//        Cursor cursor;
//        if (id != null) {
//            cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, "ID=?", new String[]{id}, null, null, null);
//            cursor.moveToFirst();
//        } else {
//            cursor = database.query(SQLiteDBHelper.TABLE_NAME, null, null, null, null, null, null);
//            cursor.moveToLast();
//        }
//        if (cursor.getCount() > 0) {
//            String route = cursor.getString(cursor.getColumnIndex("ROUTE"));
//            String[] markers_str = route.split(";");
//            for (int i = 0; i < markers_str.length; i++) {
//                String[] location = markers_str[i].split(",");
//                LatLng latLng = new LatLng(Double.parseDouble(location[0]), Double.parseDouble(location[1]));
//                if (i == 0) {
//                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(latLng, 18, 0, 0));
//                    aMap.animateCamera(cameraUpdate);
//                }
//                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
//                markerOptions.title(String.valueOf(markerLists.size() + 1));
//                markerOptions.snippet("纬度：" + latLng.latitude + "\n经度：" + latLng.longitude);
//                markerOptions.anchor(0.5f, 0.5f);
//                markerOptions.draggable(true);
//                markerOptions.setFlat(true);
//                if (markerLists.size() > 0) {
//                    LatLng lastLatlng = markerLists.get(markerLists.size() - 1).getPosition();
//                    polylineLists.add(aMap.addPolyline(new PolylineOptions().add(latLng, lastLatlng).width(14).color(Color.parseColor("#0B76CE"))));
//                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
//                } else {
//                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao_start)));
//                }
//                markerLists.add(aMap.addMarker(markerOptions));
//            }
//        }
//        cursor.close();
//        database.close();
//    }

//    private void morph(int state) {
//        if (state != NONE) {
//            this.state = state;
//            this.markEnable = false;
//            btnEnable.setCompoundDrawables(null, picMarkDisable, null, null);
//            tvToolbar.setText(toolbarTitle);
//            hideAll();
//        }
//        switch (state) {
//            case UNREADY:
//                Toasty.error(activity, "连接中断，请重新连接", Toast.LENGTH_SHORT).show();
//                btnPoweron.setVisibility(View.VISIBLE);
//                preState = Integer.MAX_VALUE;
//                mqttService.close();
//                resetMap();
//                tvBattery.setVisibility(View.INVISIBLE);
//                fam.hideMenu(false);
//                break;
//            case READY:
//                llMark.setVisibility(View.VISIBLE);
//                llMethod.setVisibility(View.VISIBLE);
//                btnHome.setVisibility(View.VISIBLE);
//                fam.showMenu(true);
//                break;
//            case GONE:
//                btnGoStop.setText("暂停");
//                btnGoStop.setCompoundDrawables(null, picPause, null, null);
//                llNav.setVisibility(View.VISIBLE);
//                tvToolbar.setText(swNav.getSelectedTab() == 0 ? "正处于单次自主导航" : "正处于循环自主导航");
//                tvCircle.setVisibility(swNav.getSelectedTab() == 0 ? View.GONE : View.VISIBLE);
//                fam.showMenu(true);
//                break;
//            case PAUSE:
//                btnGoStop.setText("开始");
//                btnGoStop.setCompoundDrawables(null, picStart, null, null);
//                llNav.setVisibility(View.VISIBLE);
//                tvToolbar.setText(swNav.getSelectedTab() == 0 ? "正处于单次自主导航" : "正处于循环自主导航");
//                tvCircle.setVisibility(swNav.getSelectedTab() == 0 ? View.GONE : View.VISIBLE);
//                fam.showMenu(true);
//                break;
//            case HOMING:
//                llHome.setVisibility(View.VISIBLE);
//                fam.showMenu(true);
//                break;
//            case FINISH:
//                llFinish.setVisibility(View.VISIBLE);
//                mqttSendThreadPool.execute(new QueryTimeDisThread());
//                fam.showMenu(true);
//                break;
//            default:
//                break;
//        }
//    }

    private void hideAll() {
        llMark.setVisibility(View.GONE);
        llMethod.setVisibility(View.INVISIBLE);
        llNav.setVisibility(View.GONE);
        llFinish.setVisibility(View.GONE);
        llHome.setVisibility(View.GONE);
        btnHome.setVisibility(View.GONE);
        btnPoweron.setVisibility(View.GONE);
        tvBattery.setVisibility(View.VISIBLE);
    }

    private void resetMap() {
        for (Marker marker : markerLists.get(activity.selectShip)) {
            marker.remove();
        }
        for (Polyline polyline : polylineLists.get(activity.selectShip)) {
            polyline.remove();
        }
        for (Polyline polyline : traceLists.get(activity.selectShip)) {
            polyline.remove();
        }
        shipPointLists.get(activity.selectShip).removeAll(shipPointLists.get(activity.selectShip));
        shipPointLists.get(activity.selectShip).add(new LatLng(0, 0));
        markerLists.get(activity.selectShip).removeAll(markerLists.get(activity.selectShip));
        polylineLists.get(activity.selectShip).removeAll(polylineLists.get(activity.selectShip));
        traceLists.get(activity.selectShip).removeAll(traceLists.get(activity.selectShip));
    }

    public void setBattery(int battery) {
        tvBattery.setText(String.format(Locale.getDefault(), "剩余电量：%d%%", battery));
    }

    public ArrayList<LatLng> getShipPointLists(int id) {
        return shipPointLists.get(id);
    }

    public void move(int shipid) {
        ArrayList<LatLng> shipPoints = shipPointLists.get(shipid);
        if (shipPoints.size() == 2) {
            LatLngBounds bounds = new LatLngBounds(shipPoints.get(1), shipPoints.get(shipPoints.size() - 1));
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
            CameraUpdate mCameraUpdate = CameraUpdateFactory.zoomTo(15);
            aMap.moveCamera(mCameraUpdate);
        } else {
            List<LatLng> subList = shipPoints.subList(shipPoints.size() - 2, shipPoints.size());
            SmoothMoveMarker smoothMoveMarker = smoothMoveMarkers.get(shipid);
            smoothMoveMarker.setPoints(subList);
            smoothMoveMarker.setTotalDuration(1);
            smoothMoveMarker.startSmoothMove();
            smoothMoveMarker.getMarker().setInfoWindowEnable(false);
            smoothMoveMarker.getMarker().setFlat(true);
            smoothMoveMarker.getMarker().setAnchor(0.5f, 0.5f);
            traceLists.get(shipid).add(aMap.addPolyline(new PolylineOptions().add(shipPoints.get(shipPoints.size() - 2),
                    shipPoints.get(shipPoints.size() - 1)).width(7).color(colors[shipid])));
        }
    }

//    public void handleState(int state) {
//        // 有一个想法。目前，如果用prestate，会出现Bug，即如果点了会使App状态改变的按钮，
//        // 而prestate不变，此时会导致状态错误。解决方法有两种，一种是在点击按钮的时候更改prestate，
//        // 但这种方法通用性太差。另一种方法是统一船发来的state和app的state。决定采用第二种。
////        long id = activity.getSharedPreferences("cleanship", MODE_PRIVATE).getLong("route", -1);
//        int tempState = UNREADY;
//        swNav.setSelectedTab(0);
//        switch (state) {
//            case 0:
////                开机初始状态
//                tempState = READY;
//                break;
//            case -1:
////                连线模式运行中
//                tempState = GONE;
//                break;
//            case -2:
////                循环模式暂停
//                swNav.setSelectedTab(1);
//                tempState = PAUSE;
//                break;
//            case -3:
////                连线模式暂停
//                tempState = PAUSE;
//                break;
//            case -4:
////                跑完了
//                tempState = FINISH;
//                break;
//            case -5:
////                返航
//                tempState = HOMING;
//                break;
//            case -10:
////                待机
//
//                break;
//            default:
//                break;
//        }
//        if (state > 0) {
//            tvCircle.setText(String.format(Locale.getDefault(), "第%d圈", state));
//            swNav.setSelectedTab(1);
//            tempState = GONE;
//        }
//        if (tempState != this.state) {
//            if (state != -5 && state != 0) {
////                    loadRoute(id == -1 ? null : String.valueOf(id));
//            }
//            this.state = UNREADY;
//            mHandler.sendMessage(mHandler.obtainMessage(8, tempState));
//        }
//    }

    @Override
    public void onClick(View view) {
        PopupWindow popupHistory;
        PopupWindow shipListWindow;
        final ArrayList<Marker> markers = activity.selectShip == -1 ? null : markerLists.get(activity.selectShip);
        ArrayList<Polyline> polylines = activity.selectShip == -1 ? null : polylineLists.get(activity.selectShip);
        ArrayList<Polyline> traces = activity.selectShip == -1 ? null : traceLists.get(activity.selectShip);
        switch (view.getId()) {
            case R.id.btn_poweron:
                publishMessageForResult("$poweron#", "正在开机");
                break;
            case R.id.btn_changemap:
                if (aMap.getMapType() == AMap.MAP_TYPE_NORMAL) {
                    aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                } else {
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                }
                fam.close(true);
                break;
            case R.id.btn_delete:
                new AlertDialog.Builder(activity).setTitle("提示")
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
                publishMessageForResult("$ORDER,2#");
                break;
            case R.id.btn_enable:
                markEnable = !markEnable;
                btnEnable.setCompoundDrawables(null, markEnable ? picMarkEnable : picMarkDisable, null, null);
                break;
            case R.id.btn_go:
                if (markers.size() > 0) {
                    for (Polyline line : traces) {
                        line.remove();
                    }
                    traces.removeAll(traces);
                    final StringBuilder hisBuilder = new StringBuilder();
                    showLoadingView("正在发送");
                    for (Marker marker : markers) {
                        hisBuilder.append(String.format(Locale.getDefault(), "%.6f,%.6f;", marker.getPosition().latitude, marker.getPosition().longitude));
                        publishMessage(String.format(Locale.getDefault(), "$GNGGA,%.6f,%.6f#", marker.getPosition().latitude, marker.getPosition().longitude));

                    }
                    publishMessageForResult(swNav.getSelectedTab() == 0 ? "$NAV,1#" : "$NAV,2#");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
                    final String date = dateFormat.format(new Date(System.currentTimeMillis()));
                    saveRoute(date, hisBuilder.toString(), pos);
                } else {
//                        loadRoute(null);
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
                View contentView = LayoutInflater.from(activity).inflate(R.layout.popup_history, null);
                SwipeMenuRecyclerView recyclerView = contentView.findViewById(R.id.recyclerView);
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
                int height = getResources().getDisplayMetrics().heightPixels / 2;
                if (contentView.getMeasuredHeight() > height) {
                    popupHistory.setHeight(height);
                } else {
                    popupHistory.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                popupHistory.showAsDropDown(llMethod);
                break;
            case R.id.btn_abort:
                publishMessageForResult("$CLEAR#");
                break;
            case R.id.btn_gostop:
                int temp = ships.get(activity.selectShip).getState();
                if (temp == -3 || temp == -2) {
                    publishMessage("$GO#");
                } else if (temp == -1 || temp > 0) {
                    publishMessage("$STOP#");
                }
                break;
            case R.id.btn_stop_home:
                publishMessageForResult("$CLEAR#");
                break;
            case R.id.btn_finish:
                publishMessageForResult("$CLEAR#");
                resetMap();
                break;
            case R.id.btn_reload:
                publishMessageForResult("$CLEAR#");
//                    resetMap();
//                    long id = getSharedPreferences("cleanship", MODE_PRIVATE).getLong("route", -1);
//                    loadRoute(id == -1 ? null : String.valueOf(id));
                break;
            case R.id.btn_ctl:
                fam.close(true);
                publishMessage("$ORDER,7#");
                break;
            case R.id.tv_toolbar:
                contentView = LayoutInflater.from(activity).inflate(R.layout.popup_shiplist, null);
                recyclerView = contentView.findViewById(R.id.recyclerView);
                shipListWindow = new PopupWindow();
                shipListWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
                shipListWindow.setOutsideTouchable(true);
                shipListWindow.setContentView(contentView);
                shipListWindow.setAnimationStyle(R.style.dismiss_anim);
                loadShipList(recyclerView, shipListWindow);
                contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                height = getResources().getDisplayMetrics().heightPixels / 2;
                if (contentView.getMeasuredHeight() > height) {
                    shipListWindow.setHeight(height);
                } else {
                    shipListWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                shipListWindow.showAsDropDown(toolbar);
                shipListWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        shipPopupWindowList = null;
                        shipPopupWindowAdapter = null;
                    }
                });
                break;
            default:
                break;
        }
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
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        final SettingService settingService = AndPermission.permissionSetting(activity);
                        new AlertDialog.Builder(activity)
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
                        View contentView = LayoutInflater.from(activity).inflate(R.layout.permission_ask, null);
                        contentView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                popupWindow.dismiss();
                                executor.execute();
                            }
                        });
                        popupWindow.setContentView(contentView);
                        popupWindow.setAnimationStyle(R.style.dismiss_anim);
                        btnPoweron.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!activity.isFinishing()) {
                                    popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                                }
                            }
                        });
                    }
                })
                .start();
    }

    private void loadShipList(SwipeMenuRecyclerView recyclerView, final PopupWindow shipListWindow) {
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("shipname", MODE_PRIVATE);
        shipPopupWindowList = new ArrayList<>();
        for (int i =  0; i < activity.userInfo.getTotalship(); i++) {
            String shipName = sharedPreferences.getString(String.valueOf(i), String.valueOf(i));
            Map<String, String> map = new HashMap<>();
            map.put("title", shipName);
            map.put("detail", String.valueOf(ships.get(i).getState()));
            map.put("status", String.valueOf(ships.get(i).getStatus()));
            shipPopupWindowList.add(map);
        }
        shipPopupWindowAdapter = new ShiplistAdapter(shipPopupWindowList);
        shipPopupWindowAdapter.notifyDataSetChanged();
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setSwipeItemClickListener(new SwipeItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(View itemView, int position) {
                if (position == 0) {
                    tvToolbar.setText("欧卡小蓝船");
                    loadAllShip(true);
                    newHandleState(-11);
                    tvBattery.setVisibility(View.INVISIBLE);
                }
                else {
                    tvToolbar.setText(shipPopupWindowList.get(position - 1).get("title"));
                    newHandleState(ships.get(position - 1).getState());
                    loadOneShip(activity.selectShip, position - 1);
                    tvBattery.setText("剩余电量：" + ships.get(position - 1).getBattery() + "%");
                }
                activity.selectShip = position - 1;
                topicSend = String.format(Locale.getDefault(), "APP2SHIP/%d/%d", activity.userInfo.getShip_id(), position - 1);
                shipListWindow.dismiss();
            }
        });
        recyclerView.addItemDecoration(new DefaultItemDecoration(0xBB1C1C1C));
        recyclerView.setSwipeMenuCreator(new SwipeMenuCreator() {
            @Override
            public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                SwipeMenuItem renameItem = new SwipeMenuItem(activity).setWidth((int)(metrics.widthPixels * 0.1))
                        .setImage(R.drawable.menu_rename).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                swipeRightMenu.addMenuItem(renameItem);
            }
        });
        recyclerView.setSwipeMenuItemClickListener(new SwipeMenuItemClickListener() {
            @Override
            public void onItemClick(SwipeMenuBridge menuBridge) {
                menuBridge.closeMenu();
                final int pos = menuBridge.getAdapterPosition();
//                        重命名
                final EditText etName = new EditText(activity);
                etName.setHint(shipPopupWindowList.get(pos).get("title"));
                new AlertDialog.Builder(activity)
                        .setTitle("重命名路线")
                        .setView(etName)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String name = etName.getText().toString();
                                if (name.equals("")) {
                                    return;
                                }
                                shipPopupWindowList.get(pos).put("title", name);
                                shipPopupWindowAdapter.notifyItemChanged(pos);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(String.valueOf(pos), name);
                                editor.apply();
                            }
                        })
                        .show();
            }
        });
        recyclerView.setAdapter(shipPopupWindowAdapter);
    }

    private void loadAllShip(boolean visiable) {
        for (int i = 0; i < activity.userInfo.getTotalship(); i++) {
            for (Marker marker : markerLists.get(i)) {
                marker.setVisible(visiable);
            }
            for (Polyline polyline : polylineLists.get(i)) {
                polyline.setVisible(visiable);
            }
            for (Polyline polyline : traceLists.get(i)) {
                polyline.setVisible(visiable);
            }
        }
    }

    private void loadOneShip(int oldid, int id) {
        if (oldid != -1) {
            for (Marker marker : markerLists.get(oldid)) {
                marker.setVisible(false);
            }
            for (Polyline polyline : polylineLists.get(oldid)) {
                polyline.setVisible(false);
            }
            for (Polyline polyline : traceLists.get(oldid)) {
                polyline.setVisible(false);
            }
        }
        else {
            loadAllShip(false);
        }

        for (Marker marker : markerLists.get(id)) {
            marker.setVisible(true);
        }
        for (Polyline polyline : polylineLists.get(id)) {
            polyline.setVisible(true);
        }
        for (Polyline polyline : traceLists.get(id)) {
            polyline.setVisible(true);
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
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setSwipeItemClickListener(new SwipeItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                popupHistory.dismiss();
                routeID = Long.parseLong(list.get(position).get("id"));
//                loadRoute(list.get(position).get("id"));
            }
        });
        recyclerView.setSwipeMenuCreator(new SwipeMenuCreator() {
            @Override
            public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                SwipeMenuItem deleteItem = new SwipeMenuItem(activity).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_delete).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                SwipeMenuItem renameItem = new SwipeMenuItem(activity).setWidth((int)(metrics.widthPixels * 0.2))
                        .setImage(R.drawable.menu_rename).setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                swipeRightMenu.addMenuItem(renameItem);
                swipeRightMenu.addMenuItem(deleteItem);

                SwipeMenuItem topItem = new SwipeMenuItem(activity).setWidth((int) (metrics.widthPixels * 0.2))
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
                        final EditText etName = new EditText(activity);
                        etName.setHint(list.get(pos).get("title"));
                        new AlertDialog.Builder(activity)
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

    public ArrayList<Ship> getShips() {
        return ships;
    }

    public void updateShiplist(int pos, int status) {
        if (shipPopupWindowAdapter != null) {
            shipPopupWindowList.get(pos).put("status", String.valueOf(status));
            shipPopupWindowList.get(pos).put("detail", String.valueOf(status));
            shipPopupWindowAdapter.notifyItemChanged(pos + 1);
        }
    }

    public void newHandleState(int state) {
        swNav.setSelectedTab(0);
        this.markEnable = false;
        btnEnable.setCompoundDrawables(null, picMarkDisable, null, null);
        hideAll();
        switch (state) {
            case 0:
//                开机初始状态
                llMark.setVisibility(View.VISIBLE);
                llMethod.setVisibility(View.VISIBLE);
                btnHome.setVisibility(View.VISIBLE);
                fam.showMenu(true);
                break;
            case -1:
//                连线模式运行中
                btnGoStop.setText("暂停");
                btnGoStop.setCompoundDrawables(null, picPause, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvCircle.setVisibility(View.GONE);
                fam.showMenu(true);
                break;
            case -2:
//                循环模式暂停
                swNav.setSelectedTab(1);
                btnGoStop.setText("开始");
                btnGoStop.setCompoundDrawables(null, picStart, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvCircle.setVisibility(View.VISIBLE);
                fam.showMenu(true);
                break;
            case -3:
//                连线模式暂停
                btnGoStop.setText("开始");
                btnGoStop.setCompoundDrawables(null, picStart, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvCircle.setVisibility(View.GONE);
                fam.showMenu(true);
                break;
            case -4:
//                跑完了
                llFinish.setVisibility(View.VISIBLE);
                fam.showMenu(true);
                break;
            case -5:
//                返航
                llHome.setVisibility(View.VISIBLE);
                fam.showMenu(true);
                break;
            case -10:
//                待机
                btnPoweron.setVisibility(View.VISIBLE);
                break;
            case -11:
//                关机
                break;
            default:
                break;
        }
        if (state > 0) {
            swNav.setSelectedTab(1);
            btnGoStop.setText("暂停");
            btnGoStop.setCompoundDrawables(null, picPause, null, null);
            llNav.setVisibility(View.VISIBLE);
            tvCircle.setVisibility(View.VISIBLE);
            fam.showMenu(true);
            tvCircle.setText(String.format(Locale.getDefault(), "第%d圈", state));
        }
    }

    private void publishMessage(String data) {
        try {
            mqttClient.publish(topicSend, (data + "\r\n").getBytes(), 2, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publishMessageForResult(String data) {
        publishMessageForResult(data, "正在发送");
    }

    private void publishMessageForResult(String data, String hint) {
        showLoadingView(hint);
        publishMessage(data);
        mqttSendThreadPool.execute(new mqttSendThread());
    }

    private class mqttSendThread implements Runnable {
        @Override
        public void run() {
            int count = 0;
            while (loadingView.isShowing()) {
                count++;
                if (count > 5) {
                    loadingView.dismiss();
                    mHandler.sendMessage(mHandler.obtainMessage(0, "发送失败"));
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class MyHandler extends Handler {
        WeakReference<MapFragment> mFragment;

        MyHandler(MapFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MapFragment mapFragment = mFragment.get();
            MainActivity activity = (MainActivity) mapFragment.getActivity();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    Toasty.info(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toasty.success(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toasty.error(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }
}

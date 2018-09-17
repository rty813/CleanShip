package com.cn.orcatech.cleanship.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.cn.orcatech.cleanship.MyReceiver;
import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.Ship;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.cn.orcatech.cleanship.mqtt.MqttService;
import com.cn.orcatech.cleanship.util.BoundUtils;
import com.cn.orcatech.cleanship.util.SQLiteDBHelper;
import com.cn.orcatech.cleanship.util.WriteThreadFactory;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    public MqttClient mqttClient;
    public MyHandler mHandler;
    private MapView mMapView;
    private AMap aMap;
    private boolean markEnable = false;
    private ArrayList<ArrayList<Marker>> markerLists;
    private ArrayList<ArrayList<Polyline>> polylineLists;
    private ArrayList<ArrayList<Polyline>> traceLists;
    private int[] colors;
    private MyReceiver myReceiver;
    private Drawable picPause;
    private Drawable picStart;
    private Drawable picMarkEnable;
    private Drawable picMarkDisable;
    private Button btnPoweron;
    private LinearLayout llFinish;
    private LinearLayout llNav;
    private LinearLayout llMark;
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
    private Button btnAbort;
    private Button btnEnable;
    private TextView tvFinish;
    private TextView tvToolbar;
    private TextView tvDate;
    private TextView tvCircle;
    private ExecutorService mqttSendThreadPool;
    private MainActivity activity;
    private boolean asyncTaskFlag = false;
    public static ArrayList<Ship> ships;
    public MqttCallback mqttCallBack = new MqttCallback() {
        @Override
        public void messageArrived(String topic, MqttMessage message) {
            if (message != null) {
                Log.d(TAG, "messageArrived, topic: " + topic + "; message: " + new String(message.getPayload()));
                if (message.toString().contains("ACK1")) {
                    mHandler.sendMessage(mHandler.obtainMessage(1, "发送成功"));
                    if (asyncTaskFlag) {
                        asyncTaskFlag = false;
                    }
                    else if (loadingView.isShowing()) {
                        loadingView.dismiss();
                    }
                    return;
                }
                else if (message.toString().contains("ACK0")) {
                    mHandler.sendMessage(mHandler.obtainMessage(2, "发送失败"));
                    if (loadingView.isShowing()) {
                        asyncTaskFlag = false;
                        loadingView.dismiss();
                    }
                    return;
                }
                String[] topics = topic.split("/");
                try {
                    int ship_id = Integer.parseInt(topics[2]);
                    if (ship_id > activity.userInfo.getTotalship() - 1) {
                        return;
                    }
                    String data = message.toString().trim();
                    if (data.startsWith("$") && data.endsWith("#")) {
                        data = data.replaceAll("#", "");
                        data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                        if (!"".equals(data)) {
                            String[] strings = data.split(";");
                            if (strings.length < 10) {
                                return;
                            }
                            final String finalData = data.replace(";", "\n");
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.getDataFragment().setData(finalData);
                                }
                            });
                            Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                            intent.putExtra("shipid", ship_id);
                            intent.putExtra("state", strings[0]);
                            intent.putExtra("latlng", strings[1]);
                            intent.putExtra("yaw", strings[2]);
                            intent.putExtra("pdPercent", strings[3]);
                            intent.putExtra("pdRematime", strings[4]);
                            intent.putExtra("pdCurrent", strings[5]);
                            intent.putExtra("gps_speed", strings[6]);
                            intent.putExtra("gps_stars", strings[7]);
                            intent.putExtra("temperature", strings[9]);
                            activity.sendBroadcast(intent);
                        }
                    } else {
                        mHandler.sendMessage(mHandler.obtainMessage(3, data));
                    }
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
            mHandler.sendMessage(mHandler.obtainMessage(2, "连接中断！"));
            hideAll();
            activity.logout();
        }
    };
    public String topicSend = "";
    private ArrayList<Polygon> polygons;
    private Button btnCancel;

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
        btnVel = view.findViewById(R.id.btn_vel);
        btnAbort = view.findViewById(R.id.btn_abort);
        btnEnable = view.findViewById(R.id.btn_enable);
        seekBar = view.findViewById(R.id.seekbar);
        btnCancel = view.findViewById(R.id.btn_cancel);
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
                publishMessage(String.format(Locale.getDefault(), "$ORDER,6,%d#", seekBar.getProgress()));
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
        view.findViewById(R.id.btn_changemap).setOnClickListener(this);
        tvToolbar.setOnClickListener(this);
        btnAbort.setOnClickListener(this);
        btnHome.setOnClickListener(this);
        btnGoStop.setOnClickListener(this);
        btnEnable.setOnClickListener(this);
        btnPoweron.setOnClickListener(this);

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
        llNav.setOnTouchListener(onTouchListener);
    }

    public void initClass(int totalship) {
        shipPointLists = new ArrayList<>(totalship);
        polylineLists = new ArrayList<>(totalship);
        traceLists = new ArrayList<>(totalship);
        markerLists = new ArrayList<>(totalship);
        smoothMoveMarkers = new ArrayList<>();
        polygons = new ArrayList<>();
        ships = new ArrayList<>();
        for (int i = 0; i < totalship; i++) {
            ships.add(new Ship());
            shipPointLists.add(new ArrayList<LatLng>());
            polylineLists.add(new ArrayList<Polyline>());
            traceLists.add(new ArrayList<Polyline>());
            markerLists.add(new ArrayList<Marker>());
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
        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
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
                    Polyline polyline = aMap.addPolyline(new PolylineOptions().add(latLng, marker.getPosition()).width(14).color(Color.parseColor("#0B76CE")));
                    polylineLists.get(activity.selectShip).add(polyline);
                    for (Polygon polygon : polygons) {
                        if (BoundUtils.detectIntersect(polyline, polygon)) {
                            mHandler.sendMessage(mHandler.obtainMessage(3, "航迹穿过边界或障碍！"));
                            btnCancel.performClick();
                            break;
                        }
                    }
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

                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                        markerLists.get(activity.selectShip).size() > 0 ? R.drawable.mao : R.drawable.mao_start)));
                markerLists.get(activity.selectShip).add(aMap.addMarker(markerOptions));
                if (markerLists.get(activity.selectShip).size() > 1) {
                    LatLng lastLatlng = markerLists.get(activity.selectShip).get(markerLists.get(activity.selectShip).size() - 2).getPosition();
                    Polyline polyline = aMap.addPolyline(new PolylineOptions().add(latLng, lastLatlng).width(14).color(Color.parseColor("#0B76CE")));
                    polylineLists.get(activity.selectShip).add(polyline);
                    for (Polygon polygon : polygons) {
                        if (BoundUtils.detectIntersect(polyline, polygon)) {
                            mHandler.sendMessage(mHandler.obtainMessage(3, "航迹穿过边界或障碍！"));
                            btnCancel.performClick();
                            break;
                        }
                    }
                }
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

    private void hideAll() {
        llMark.setVisibility(View.GONE);
        llNav.setVisibility(View.GONE);
        llFinish.setVisibility(View.GONE);
        llHome.setVisibility(View.GONE);
        btnHome.setVisibility(View.GONE);
        btnPoweron.setVisibility(View.GONE);
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
        markerLists.get(activity.selectShip).removeAll(markerLists.get(activity.selectShip));
        polylineLists.get(activity.selectShip).removeAll(polylineLists.get(activity.selectShip));
        traceLists.get(activity.selectShip).removeAll(traceLists.get(activity.selectShip));
    }

    public ArrayList<LatLng> getShipPointLists(int id) {
        return shipPointLists.get(id);
    }

    public void move(int shipid) {
        ArrayList<LatLng> shipPoints = shipPointLists.get(shipid);
        List<LatLng> subList = new ArrayList<>();
        if (shipPoints.size() == 1) {
            subList.add(shipPoints.get(0));
            subList.add(shipPoints.get(0));
        }
        else {
            subList = shipPoints.subList(shipPoints.size() - 2, shipPoints.size());
            traceLists.get(shipid).add(aMap.addPolyline(new PolylineOptions().add(shipPoints.get(shipPoints.size() - 2),
                    shipPoints.get(shipPoints.size() - 1)).width(7).color(colors[shipid])));
        }
        SmoothMoveMarker smoothMoveMarker = smoothMoveMarkers.get(shipid);
        smoothMoveMarker.setPoints(subList);
        smoothMoveMarker.setTotalDuration(1);
        smoothMoveMarker.startSmoothMove();
        smoothMoveMarker.getMarker().setInfoWindowEnable(false);
        smoothMoveMarker.getMarker().setFlat(true);
        smoothMoveMarker.getMarker().setAnchor(0.5f, 0.5f);
    }

    public void moveCamera(int shipid) {
        ArrayList<LatLng> shipPoints = shipPointLists.get(shipid);
        int len = shipPoints.size();
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
                shipPoints.get(len - 1), shipPoints.get(len - 1)), 50));
    }

    @Override
    public void onClick(View view) {
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
                    marker.destroy();
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
                    loadingView = new ProgressDialog(activity);
                    loadingView.setTitle("发送中");
                    loadingView.setCanceledOnTouchOutside(false);
                    loadingView.setCancelable(false);
                    loadingView.setMax(markers.size());
                    loadingView.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    loadingView.setIcon(R.mipmap.ic_launcher);
                    loadingView.show();
                    new MyAsyncTask(this).execute();
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
        moveCamera(id);
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
                btnHome.setVisibility(View.VISIBLE);
                break;
            case -1:
//                连线模式运行中
                btnGoStop.setText("暂停");
                btnGoStop.setCompoundDrawables(null, picPause, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvCircle.setVisibility(View.GONE);
                break;
            case -2:
//                循环模式暂停
                swNav.setSelectedTab(1);
                btnGoStop.setText("开始");
                btnGoStop.setCompoundDrawables(null, picStart, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvCircle.setVisibility(View.VISIBLE);
                break;
            case -3:
//                连线模式暂停
                btnGoStop.setText("开始");
                btnGoStop.setCompoundDrawables(null, picStart, null, null);
                llNav.setVisibility(View.VISIBLE);
                tvCircle.setVisibility(View.GONE);
                break;
            case -4:
//                跑完了
                llFinish.setVisibility(View.VISIBLE);
                queryTaskInfo();
                break;
            case -5:
//                返航
                llHome.setVisibility(View.VISIBLE);
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
            tvCircle.setText(String.format(Locale.getDefault(), "第%d圈", state));
        }
    }

    private void queryTaskInfo() {
        if (activity.selectShip > 0) {
            btnAbort.postDelayed(new Runnable() {
                @Override
                public void run() {
                    StringRequest request = new StringRequest("http://orca-tech.cn/app/taskhistory.php", RequestMethod.POST);
                    request.add("ship_id", activity.userInfo.getShip_id()).add("id", activity.selectShip).add("limit", 1).add("type", "select");
                    AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
                        @Override
                        public void onSucceed(int what, final Response<String> response) {
                            super.onSucceed(what, response);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("导航结果")
                                            .setMessage(response.get())
                                            .show();
                                }
                            });
                        }
                    });
                }
            }, 1000);

        }
    }

    public void publishMessage(String data) {
        try {
            mqttClient.publish(topicSend, (data + "\r\n").getBytes(), 2, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishMessageForResult(String data) {
        publishMessageForResult(data, "正在发送");
    }

    private void publishMessageForResult(String data, String hint) {
        showLoadingView(hint);
        publishMessage(data);
        mqttSendThreadPool.execute(new mqttSendThread());
    }

    public void loadBound() {
        for (Polygon polygon : polygons) {
            polygon.remove();
        }
        polygons.clear();
        StringRequest request = new StringRequest("http://orca-tech.cn/app/bound.php", RequestMethod.POST);
        request.add("ship_id", activity.userInfo.getShip_id()).add("type", "select");
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            private PolygonOptions initPolygonOptions(int flag) {
                PolygonOptions options = new PolygonOptions();
                if (flag == 0) {
                    options.strokeWidth(15).strokeColor(Color.argb(150, 1, 1, 1))
                            .fillColor(Color.argb(30, 0, 0, 0));
                } else {
                    options.strokeWidth(15).strokeColor(Color.argb(255, 255, 0, 0))
                            .fillColor(Color.argb(200, 255, 60, 60));
                }
                return options;
            }

            @Override
            public void onSucceed(int what, Response<String> response) {
                super.onSucceed(what, response);
                try {
                    JSONArray array = new JSONArray(response.get());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String latlngs[] = obj.getString("latlng").split(";");
                        PolygonOptions options = initPolygonOptions(obj.getInt("flag"));
                        for (String latlng : latlngs) {
                            double lat = Double.parseDouble(latlng.split(",")[0]);
                            double lng = Double.parseDouble(latlng.split(",")[1]);
                            options.add(new LatLng(lat, lng));
                        }
                        polygons.add(aMap.addPolygon(options));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(int what, Response<String> response) {
                Toasty.warning(activity, "拉取失败", Toast.LENGTH_SHORT).show();
                super.onFailed(what, response);
            }
        });
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
                case 3:
                    Toasty.warning(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    private static class MyAsyncTask extends AsyncTask<Void, Integer, Void> {
        private WeakReference<MapFragment> fragment;

        MyAsyncTask(MapFragment fragment) {
            this.fragment = new WeakReference<>(fragment);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) this.fragment.get().getActivity();
            MapFragment fragment = this.fragment.get();
            final ArrayList<Marker> markers = activity.selectShip == -1 ? null : fragment.markerLists.get(activity.selectShip);
            publishMessageForResult("$CLEAR#");
            try {
                Thread.sleep(100);
                for (int i = 0; i < markers.size(); i++) {
                    double latitude = markers.get(i).getPosition().latitude;
                    double longitude = markers.get(i).getPosition().longitude;
                    boolean success = publishMessageForResult(String.format(Locale.getDefault(), "$GNGGA,%.6f,%.6f#", latitude, longitude));
                    if (!success) {
                        return null;
                    }
                    publishProgress(i + 1);
                    Thread.sleep(100);
                }
                fragment.publishMessageForResult(fragment.swNav.getSelectedTab() == 0 ? "$NAV,1#" : "$NAV,2#");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        private boolean publishMessageForResult(String data){
            MapFragment fragment = this.fragment.get();
            fragment.asyncTaskFlag = true;
            fragment.publishMessage(data);
            try {
                for (int i = 0; i < 50; i++) {
                    Thread.sleep(100);
                    if (!fragment.asyncTaskFlag) {
                        return true;
                    }
                    if (!fragment.loadingView.isShowing()) {
                        return false;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            fragment.asyncTaskFlag = false;
            fragment.loadingView.dismiss();
            fragment.mHandler.sendMessage(fragment.mHandler.obtainMessage(2, "发送失败"));
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            fragment.get().loadingView.setProgress(values[0]);
            super.onProgressUpdate(values);
        }
    }

    public void handleToolbarSelect(int pos) {
        if (pos == 0) {
            loadAllShip(true);
            newHandleState(-11);
        }
        else {
            newHandleState(ships.get(pos - 1).getState());
            loadOneShip(activity.selectShip, pos - 1);
        }
    }
}

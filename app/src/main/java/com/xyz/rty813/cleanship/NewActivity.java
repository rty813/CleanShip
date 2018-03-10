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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.PopupWindow;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import app.dinus.com.loadingdrawable.LoadingView;

public class NewActivity extends AppCompatActivity implements View.OnClickListener, SerialPortTool.onConnectedListener {
    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int GONE = 2;
    private static final int NAV = 3;
    private static final int NONE = -1;
    private static final int PAUSE = 4;
    private static final String MY_APPID = "2882303761517676503";
    private static final String MY_APP_KEY = "5131767662503";
    private static final String CHANNEL = "SELF";
    private static final int BAUD_RATE = 115200;

    public static SQLiteDBHelper dbHelper;
    private AMap aMap;
    private MapView mMapView;
    private long mExitTime;
    private PopupWindow popupWindow;
    private SerialPortTool serialPort;
    private int state;
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
    private Drawable picPause;
    private Drawable picStart;
    private Drawable picCalc;
    private Drawable picNav;
    private Drawable picWorking;
    private Button btnConnect;

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

        findViewById(R.id.fab_plane).setOnClickListener(this);
        findViewById(R.id.fab_satellite).setOnClickListener(this);
        findViewById(R.id.btn_connect).setOnClickListener(this);
    }

    private void initAMap() {

        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        aMap.getUiSettings().setCompassEnabled(true);
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
        switch (view.getId()) {
            case R.id.btn_connect:
                showLoadingView();
                initSerialPort();
                break;
        }
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
//            btnCalc.setEnabled(true);
//            btnGostop.setEnabled(true);
//            btnRoute.setEnabled(true);
//            btnMore.setEnabled(true);
        } else {
            this.state = UNREADY;
//            btnCalc.setEnabled(false);
//            btnGostop.setEnabled(false);
//            btnMore.setEnabled(false);
//            btnRoute.setEnabled(false);
        }
        switch (state) {
            case UNREADY:
//                Toast.makeText(this, "连接中断，请重新连接", Toast.LENGTH_SHORT).show();
                break;
//            case READY:
//                btnGostop.setText("暂停");
//                btnGostop.setCompoundDrawables(null, picPause, null, null);
//                btnGostop.setEnabled(false);
//                btnCalc.setText("计算");
//                btnCalc.setCompoundDrawables(null, picCalc, null, null);
//                break;
//            case NAV:
//                btnGostop.setText("暂停");
//                btnGostop.setCompoundDrawables(null, picPause, null, null);
//                btnGostop.setEnabled(false);
//                btnCalc.setText("导航");
//                btnCalc.setCompoundDrawables(null, picNav, null, null);
//                break;
//            case GONE:
//                btnGostop.setText("暂停");
//                btnGostop.setCompoundDrawables(null, picPause, null, null);
//                btnGostop.setEnabled(true);
//                btnCalc.setText("正在导航");
//                btnCalc.setCompoundDrawables(null, picWorking, null, null);
//                break;
//            case PAUSE:
//                btnGostop.setText("开始");
//                btnGostop.setCompoundDrawables(null, picStart, null, null);
//                btnGostop.setEnabled(true);
//                btnCalc.setText("正在导航");
//                btnCalc.setCompoundDrawables(null, picWorking, null, null);
//                break;
        }
    }

    private void showLoadingView() {
        state = UNREADY;
        findViewById(R.id.loadingview).setVisibility(View.VISIBLE);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        findViewById(R.id.loadingview).startAnimation(animation);
    }

    @Override
    public void onConnected() {
        mHandler.sendMessage(mHandler.obtainMessage(8, READY));
        new Thread(new QueryThread()).start();
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
                        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
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
        cv.put("ADDRESS", address);
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
        if (resultCode == RESULT_OK) {
            if (requestCode == 200) {
                if (data.getStringExtra("id") != null) {
                    loadRoute(data.getStringExtra("id"));
                }
            }
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
                        Thread.sleep(1000);
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

    private class WriteSerialThread implements Runnable {
        private final String mData;
        private final int mState;
        private final int mPreState;

        WriteSerialThread(NewActivity activity, String data, int state) {
            mData = data;
            mState = state;
            mPreState = activity.state;
            showLoadingView();
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
                    Thread.sleep(1000);
                } while (true);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } finally {
                mHandler.sendEmptyMessage(7);
            }
        }
    }
}

package com.cn.orcatech.cleanship.activity;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.UserInfo;
import com.cn.orcatech.cleanship.adapter.ShiplistAdapter;
import com.cn.orcatech.cleanship.fragment.ControlFragment;
import com.cn.orcatech.cleanship.fragment.DataFragment;
import com.cn.orcatech.cleanship.fragment.HistoryFragment;
import com.cn.orcatech.cleanship.fragment.LoginFragment;
import com.cn.orcatech.cleanship.fragment.MapFragment;
import com.cn.orcatech.cleanship.fragment.UserInfoFragment;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
import com.yanzhenjie.fragment.CompatActivity;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.NoHttp;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;
import com.yanzhenjie.recyclerview.swipe.SwipeItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenu;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;
import com.yanzhenjie.recyclerview.swipe.widget.DefaultItemDecoration;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import es.dmoral.toasty.Toasty;

public class MainActivity extends CompatActivity implements View.OnClickListener {

    private static final String MQTT_SERVER_URL = "tcp://orca-tech.cn:11883";
    private FragmentManager fm;
    private ArrayList<NoFragment> fragmentList;
    public static boolean hasLogin = false;
    public UserInfo userInfo;
    private static final String MY_APPID = "2882303761517784606";
    private static final String MY_APP_KEY = "5451778422606";
    private static final String CHANNEL = "SELF";
    private long mExitTime = 0;
    public int selectShip = -1;
    private Toolbar toolbar;
    public TextView tvToolbar;
    private ArrayList<Map<String, String>> shipPopupWindowList;
    private ShiplistAdapter shipPopupWindowAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        tvToolbar = findViewById(R.id.tv_toolbar);

        tvToolbar.setOnClickListener(this);

        MiStatInterface.initialize(this, MY_APPID, MY_APP_KEY, CHANNEL);
        MiStatInterface.setUploadPolicy(MiStatInterface.UPLOAD_POLICY_REALTIME, 0);
        MiStatInterface.enableExceptionCatcher(true);
        URLStatsRecorder.enableAutoRecord();
        NoHttp.initialize(this);
        userInfo = new UserInfo();
        BottomNavigationBar navigationBar = findViewById(R.id.bottom_navigation);

        fm = getSupportFragmentManager();
        fragmentList = new ArrayList<>();
        fragmentList.add(new MapFragment());
        fragmentList.add(new DataFragment());
        fragmentList.add(new HistoryFragment());
        fragmentList.add(new ControlFragment());
        fragmentList.add(new LoginFragment());
        fragmentList.add(new UserInfoFragment());

        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fragmentList) {
            transaction.add(R.id.container, fragment).hide(fragment);
        }
        transaction.show(fragmentList.get(0));
        transaction.commit();

        navigationBar.setMode(BottomNavigationBar.MODE_DEFAULT);
        navigationBar.setBackgroundStyle(BottomNavigationBar.BACKGROUND_STYLE_STATIC);
        navigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                hideAllFragments();
                FragmentTransaction transaction = fm.beginTransaction();
                if (position == 4 && hasLogin) {
                    transaction.show(fragmentList.get(5));
                }
                else {
                    transaction.show(fragmentList.get(position));
                }
                transaction.commit();
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {

            }
        });
        navigationBar.addItem(new BottomNavigationItem(R.drawable.map_plane, "地图"))
                .addItem(new BottomNavigationItem(R.drawable.bottom_data, "数据"))
                .addItem(new BottomNavigationItem(R.drawable.history, "历史"))
                .addItem(new BottomNavigationItem(R.drawable.app_ctl, "控制"))
                .addItem(new BottomNavigationItem(R.drawable.store, "个人"))
                .setFirstSelectedPosition(0)
                .initialise();
    }

    @Override
    protected int fragmentLayoutId() {
        return R.id.container;
    }

    private void hideAllFragments() {
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fragmentList) {
            transaction.hide(fragment);
        }
        transaction.commit();
    }

    public void loginSuccess() {
        hasLogin = true;
        ((UserInfoFragment) fragmentList.get(5)).setUserinfo(userInfo);
        MapFragment mapFragment = getMapFragment();
        mapFragment.initClass(userInfo.getTotalship());
        getShipInfo();
        if (fragmentList.get(4).isVisible()) {
            fm.beginTransaction()
                    .hide(fragmentList.get(4))
                    .show(fragmentList.get(5))
                    .commit();
        }
        try {
            mapFragment.mqttClient = new MqttClient(MQTT_SERVER_URL, "APP", null);
            MqttClient mqttClient = mapFragment.mqttClient;
            mqttClient.setCallback(mapFragment.mqttCallBack);
            mqttClient.connect();
            mqttClient.subscribe("SHIP2APP/" + userInfo.getShip_id() + "/#");
            Toasty.info(this, "已建立MQTT连接", Toast.LENGTH_SHORT).show();
        } catch (MqttException e) {
            e.printStackTrace();
            Toasty.error(this, "与MQTT服务器连接失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void getShipInfo() {
        StringRequest request = new StringRequest("http://orca-tech.cn/app/data_select.php", RequestMethod.POST);
        request.add("ship_id", userInfo.getShip_id());
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            @Override
            public void onSucceed(int what, Response<String> response) {
                super.onSucceed(what, response);
                try {
                    JSONArray array = new JSONArray(response.get());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject objShip = array.getJSONObject(i);
                        MapFragment.ships.get(i).setLat(objShip.getDouble("lat"));
                        MapFragment.ships.get(i).setLng(objShip.getDouble("lng"));
                        MapFragment.ships.get(i).setBattery(objShip.getInt("pd_percent"));
                        MapFragment.ships.get(i).setName(objShip.getString("name"));
                        getMapFragment().getShipPointLists(i).add(new LatLng(MapFragment.ships.get(i).getLat(), MapFragment.ships.get(i).getLng()));
                        getMapFragment().move(i);
                    }
                    Toasty.success(MainActivity.this, "数据更新成功", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailed(int what, Response<String> response) {
                super.onFailed(what, response);
                Toasty.error(MainActivity.this, "数据更新失败", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void logout() {
        hasLogin = false;
        if (fragmentList.get(5).isVisible()) {
            fm.beginTransaction()
                    .hide(fragmentList.get(5))
                    .show(fragmentList.get(4))
                    .commit();
        }
        userInfo = new UserInfo();
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

    public MapFragment getMapFragment() {
        return (MapFragment) fragmentList.get(0);
    }

    public DataFragment getDataFragment() { return (DataFragment) fragmentList.get(1); }

    public HistoryFragment getHistoryFragment() {
        return (HistoryFragment) fragmentList.get(2);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.tv_toolbar) {
            View contentView = LayoutInflater.from(this).inflate(R.layout.popup_shiplist, null);
            SwipeMenuRecyclerView recyclerView = contentView.findViewById(R.id.recyclerView);
            PopupWindow shipListWindow = new PopupWindow();
            shipListWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            shipListWindow.setOutsideTouchable(true);
            shipListWindow.setContentView(contentView);
            shipListWindow.setAnimationStyle(R.style.dismiss_anim);
            loadShipList(recyclerView, shipListWindow);
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int height = getResources().getDisplayMetrics().heightPixels / 2;
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
        }
    }

    private void loadShipList(SwipeMenuRecyclerView recyclerView, final PopupWindow shipListWindow) {
        final SharedPreferences sharedPreferences = getSharedPreferences("shipname", MODE_PRIVATE);
        shipPopupWindowList = new ArrayList<>();
        for (int i =  0; i < userInfo.getTotalship(); i++) {
            Map<String, String> map = new HashMap<>();
            map.put("title", MapFragment.ships.get(i).getName());
            map.put("detail", String.valueOf(MapFragment.ships.get(i).getState()));
            map.put("status", String.valueOf(MapFragment.ships.get(i).getStatus()));
            shipPopupWindowList.add(map);
        }
        shipPopupWindowAdapter = new ShiplistAdapter(shipPopupWindowList);
        shipPopupWindowAdapter.notifyDataSetChanged();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setSwipeItemClickListener(new SwipeItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(View itemView, int position) {
                getMapFragment().handleToolbarSelect(position);
                selectShip = position - 1;
                getHistoryFragment().update(MainActivity.this, selectShip);
                getMapFragment().topicSend = String.format(Locale.getDefault(), "APP2SHIP/%d/%d", userInfo.getShip_id(), position - 1);
                tvToolbar.setText(position == 0 ? "欧卡小蓝船" : shipPopupWindowList.get(position - 1).get("title"));
                shipListWindow.dismiss();
            }
        });
        recyclerView.addItemDecoration(new DefaultItemDecoration(0xBB1C1C1C));
        recyclerView.setSwipeMenuCreator(new SwipeMenuCreator() {
            @Override
            public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                SwipeMenuItem renameItem = new SwipeMenuItem(MainActivity.this).setWidth((int)(metrics.widthPixels * 0.1))
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
                final EditText etName = new EditText(MainActivity.this);
                etName.setHint(shipPopupWindowList.get(pos).get("title"));
                new AlertDialog.Builder(MainActivity.this)
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

    public void hideToolbar(int visibility) {
        toolbar.setVisibility(visibility);
    }

    public void updateShiplist(int pos, int status) {
        if (shipPopupWindowAdapter != null) {
            shipPopupWindowList.get(pos).put("status", String.valueOf(status));
            shipPopupWindowList.get(pos).put("detail", String.valueOf(status));
            shipPopupWindowAdapter.notifyItemChanged(pos + 1);
        }
    }

    public void lowPdNotify(int shipid) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("2", "低电量", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(this, "2");
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setContentIntent(PendingIntent.getActivity(this, 3,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle(MapFragment.ships.get(shipid).getName())
                .setContentText("低电量：" + MapFragment.ships.get(shipid).getBattery() + "%")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();
        notificationManager.notify(shipid, notification);
    }
}

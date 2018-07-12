package com.cn.orcatech.cleanship.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.widget.Toast;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.UserInfo;
import com.cn.orcatech.cleanship.fragment.DataFragment;
import com.cn.orcatech.cleanship.fragment.LoginFragment;
import com.cn.orcatech.cleanship.fragment.MapFragment;
import com.cn.orcatech.cleanship.fragment.UserInfoFragment;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.URLStatsRecorder;
import com.yanzhenjie.fragment.CompatActivity;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.NoHttp;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class MainActivity extends CompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        fragmentList.add(new LoginFragment());
        fragmentList.add(new UserInfoFragment());

        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fragmentList) {
            transaction.add(R.id.container, fragment).hide(fragment);
        }
        transaction.show(fragmentList.get(0));
        transaction.commit();

        navigationBar.setMode(BottomNavigationBar.MODE_FIXED);
        navigationBar.setBackgroundStyle(BottomNavigationBar.BACKGROUND_STYLE_STATIC);
        navigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                hideAllFragments();
                FragmentTransaction transaction = fm.beginTransaction();
                if (position == 2 && hasLogin) {
                    transaction.show(fragmentList.get(3));
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
        ((UserInfoFragment)fragmentList.get(3)).setUserinfo(userInfo);
        MapFragment mapFragment = (MapFragment) fragmentList.get(0);
//        ships = new ArrayList<>();
//        for (int i = 0; i < userInfo.getTotalship(); i++) {
//            ships.add(new Ship());
//        }
//        mapFragment.setShips(ships);
        mapFragment.initClass(userInfo.getTotalship());
        if (fragmentList.get(2).isVisible()) {
            fm.beginTransaction()
                    .hide(fragmentList.get(2))
                    .show(fragmentList.get(3))
                    .commit();
        }
        try {
            mapFragment.mqttClient = new MqttClient(MQTT_SERVER_URL, "APP", null);
            MqttClient mqttClient = mapFragment.mqttClient;
            mqttClient.setCallback(mapFragment.mqttCallBack);
            mqttClient.connect();
            for (int i = 0; i < userInfo.getTotalship(); i++){
                mqttClient.subscribe("SHIP2APP_" + userInfo.getShip_id() + "_" + i);
            }
            Toasty.info(this, "已建立MQTT连接", Toast.LENGTH_SHORT).show();
//            sendBroadcast(new Intent(MqttService.MQTT_ONCONNCET));
        } catch (MqttException e) {
            e.printStackTrace();
            Toasty.error(this, "与MQTT服务器连接失败", Toast.LENGTH_SHORT).show();
        }
    }

    public void logout() {
        SharedPreferences.Editor editor = getSharedPreferences("userinfo", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        hasLogin = false;
        if (fragmentList.get(3).isVisible()) {
            fm.beginTransaction()
                    .hide(fragmentList.get(3))
                    .show(fragmentList.get(2))
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
}

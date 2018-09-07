package com.cn.orcatech.cleanship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.cn.orcatech.cleanship.fragment.MapFragment;

import java.util.Locale;

import es.dmoral.toasty.Toasty;

/**
 * @author doufu
 */
public class MyReceiver extends BroadcastReceiver {
    public static final String ACTION_DATA_RECEIVED = "com.xyz.rty813.cleanship.ACTION_DATA_RECEIVED";
    private MapFragment mMapFragment;

    public MyReceiver() {}

    public MyReceiver(MapFragment mMapFragment){
        this.mMapFragment = mMapFragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity activity = (MainActivity) context;
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            int current = intent.getExtras().getInt(BatteryManager.EXTRA_LEVEL);
            int total = intent.getExtras().getInt(BatteryManager.EXTRA_SCALE);
            int plugged = intent.getExtras().getInt(BatteryManager.EXTRA_PLUGGED);
            if (plugged == 0) {
                if (current * 100 / total < 10) {
                    Toasty.error(context, "电量过低！请立即返航！", Toast.LENGTH_LONG).show();
                } else if (current * 100 / total < 20) {
                    Toasty.warning(context, "电量低，请及时充电", Toast.LENGTH_LONG).show();
                }
            }
        } else if (intent.getAction().equals(ACTION_DATA_RECEIVED)) {
            try {
                int shipid = intent.getIntExtra("shipid", -1);
                int state = Integer.parseInt(intent.getStringExtra("state"));
                int pdPercent = Integer.parseInt(intent.getStringExtra("pdPercent"));
                String[] latlng = intent.getStringExtra("latlng").split(",");
                double lat = Double.parseDouble(latlng[0]);
                double lng = Double.parseDouble(latlng[1]);
                float yaw = Float.parseFloat(intent.getStringExtra("yaw"));
                float temprature = Float.parseFloat(intent.getStringExtra("temprature"));
                int status;
                if (state == -10) {
                    status = 0;
                }
                else if (state == -11) {
                    status = -1;
                }
                else {
                    status = 1;
                }
//                更新坐标
                MapFragment.ships.get(shipid).setLat(lat);
                MapFragment.ships.get(shipid).setLng(lng);
                if (lat < 0 || lat > 55 || lng < 70 || lng > 136) {
                    return;
                }
                mMapFragment.getShipPointLists(shipid).add(new LatLng(lat, lng));
                mMapFragment.move(shipid);


//                更新state
                if (state != MapFragment.ships.get(shipid).getState()) {
//                    MapFragment.ships.get(shipid).setPreState(MapFragment.ships.get(shipid).getState());
                    MapFragment.ships.get(shipid).setState(state);
                    activity.updateShiplist(shipid, status);
                    if (shipid == activity.selectShip) {
                        mMapFragment.newHandleState(state);
                    }
                    if (activity.selectShip == -1 && state != -11) {
                        mMapFragment.handleToolbarSelect(shipid + 1);
                        activity.selectShip = shipid;
                        mMapFragment.topicSend = String.format(Locale.getDefault(), "APP2SHIP/%d/%d", activity.userInfo.getShip_id(), shipid);
                        activity.tvToolbar.setText(MapFragment.ships.get(shipid).getName());
                    }
                }

//                更新电量和低电量报警
                if (pdPercent != MapFragment.ships.get(shipid).getBattery()) {
                    MapFragment.ships.get(shipid).setBattery(pdPercent);
                    if (pdPercent <= 20 && pdPercent % 5 == 0) {
                        activity.lowPdNotify(shipid);
                    }
                }

//                更新温度
                MapFragment.ships.get(shipid).setTemprature(temprature);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}

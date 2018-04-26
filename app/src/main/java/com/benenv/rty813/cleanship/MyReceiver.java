package com.benenv.rty813.cleanship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;

import es.dmoral.toasty.Toasty;

public class MyReceiver extends BroadcastReceiver {
    public static final String ACTION_DATA_RECEIVED = "com.xyz.rty813.cleanship.ACTION_DATA_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        NewActivity activity = (NewActivity) context;
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            int current = intent.getExtras().getInt(BatteryManager.EXTRA_LEVEL);
            int total = intent.getExtras().getInt(BatteryManager.EXTRA_SCALE);
            int plugged = intent.getExtras().getInt(BatteryManager.EXTRA_PLUGGED);
            if (plugged == 0) {
                if (current * 100 / total < 10) {
                    Toasty.error(context, "电量过低！请立即返杭！", Toast.LENGTH_LONG).show();
                } else if (current * 100 / total < 20) {
                    Toasty.warning(context, "电量低，请及时充电", Toast.LENGTH_LONG).show();
                }
            }
        } else if (intent.getAction().equals(ACTION_DATA_RECEIVED)) {
            String data = intent.getStringExtra("data");
            if (data == null) {
                return;
            }
            String[] datas = data.split(",");
//        0=>当前经纬度   1=>目标经纬度    2=>陀螺仪方向角   3=>目标方向角    4=>当前速度
            try {
                switch (intent.getIntExtra("type", -1)) {
                    case 0:
                        double lat = Double.parseDouble(datas[0]);
                        double lng = Double.parseDouble(datas[1]);
                        LatLng latLng = activity.getShipPointList().get(activity.getShipPointList().size() - 1);
                        if ((lat < 0 || lat > 55 || lng < 70 || lng > 136)
                                || (Math.abs(lat - latLng.latitude) > 0.01) || (Math.abs(lng - latLng.longitude) > 0.01)) {
                            if (activity.getShipPointList().size() != 1) {
                                return;
                            }
                        }
                        activity.getShipPointList().add(new LatLng(lat, lng));
                        activity.move();
                        break;
                    case 7:
                        activity.handleState(Integer.parseInt(datas[0]));
                        activity.setBtnUs("1".equals(datas[1]));
                        break;
                    case 9:
                        activity.setShipCharge(Integer.parseInt(data));
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

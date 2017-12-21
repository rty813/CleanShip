package com.xyz.rty813.cleanship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amap.api.maps.model.LatLng;

public class MyReceiver extends BroadcastReceiver {
    public static final String ACTION_DATA_RECEIVED = "com.xyz.rty813.cleanship.ACTION_DATA_RECEIVED";
    private MainActivity activity;
    @Override
    public void onReceive(Context context, Intent intent) {
        activity = (MainActivity) context;
        String rawData = intent.getStringExtra("data");
        String[] datas = rawData.split(",");
//        type代表数据类型
//        0=>当前经纬度   1=>目标经纬度    2=>陀螺仪方向角   3=>目标方向角    4=>当前舵量
        switch (intent.getIntExtra("type", -1)) {
            case 0:
                Double lat = Double.parseDouble(datas[0]);
                Double lng = Double.parseDouble(datas[1]);
                activity.getShipPointList().add(new LatLng(lat, lng));
                activity.move();
                break;
            case 1:
                lat = Double.parseDouble(datas[0]);
                lng = Double.parseDouble(datas[1]);
                activity.setAimPoint(new LatLng(lat, lng));
                break;
            case 2:
                activity.setGyroAngle(rawData);
                break;
            case 3:
                activity.setAimAngle(rawData);
                break;
            case 4:
                activity.setCurrGas(rawData);
                break;
            default:
                break;
        }
    }
}

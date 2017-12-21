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
        String data = intent.getStringExtra("data");
        String rawData = intent.getStringExtra("rawData");
        if (data == null){
            return;
        }
        String[] datas = data.split(",");
        activity.setRawData(rawData);
//        type代表数据类型
//        0=>当前经纬度   1=>目标经纬度    2=>陀螺仪方向角   3=>目标方向角    4=>当前舵量
        switch (intent.getIntExtra("type", -1)) {
            case 0:
                Double lat = Double.parseDouble(datas[0]);
                Double lng = Double.parseDouble(datas[1]);
                if (lat < 0 || lat > 55 || lng < 70 || lng > 136){
                    return;
                }
                activity.setCurrLat(datas[0]);
                activity.setCurrLng(datas[1]);
                activity.getShipPointList().add(new LatLng(lat, lng));
                activity.move();
                break;
            case 1:
                lat = Double.parseDouble(datas[0]);
                lng = Double.parseDouble(datas[1]);
                activity.setAimPoint(new LatLng(lat, lng));
                break;
            case 2:
                activity.setGyroAngle(data);
                break;
            case 3:
                activity.setAimAngle(data);
                break;
            case 4:
                activity.setCurrGas(data);
                break;
            default:
                break;
        }
    }
}

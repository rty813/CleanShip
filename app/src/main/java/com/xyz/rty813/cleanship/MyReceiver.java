package com.xyz.rty813.cleanship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amap.api.maps.model.LatLng;

public class MyReceiver extends BroadcastReceiver {
    public static final String ACTION_DATA_RECEIVED = "com.xyz.rty813.cleanship.ACTION_DATA_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        NewActivity activity = (NewActivity) context;
//        activity.setRawData(intent.getStringExtra("rawData"));
        String data = intent.getStringExtra("data");
        if (data == null){
//            Toast.makeText(context, "非法数据", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] datas = data.split(",");
//        type代表数据类型
//        0=>当前经纬度   1=>目标经纬度    2=>陀螺仪方向角   3=>目标方向角    4=>当前速度
        try {
            switch (intent.getIntExtra("type", -1)) {
                case 0:
                    double lat = Double.parseDouble(datas[0]);
                    double lng = Double.parseDouble(datas[1]);
                    LatLng latLng = activity.getShipPointList().get(activity.getShipPointList().size() - 1);
                    activity.getShipPointList().add(new LatLng(lat, lng));
                    if ((lat < 0 || lat > 55 || lng < 70 || lng > 136)
                            || (Math.abs(lat - latLng.latitude) > 0.01) || (Math.abs(lng - latLng.longitude) > 0.01)) {
                        return;
                    }
//                    activity.setCurrLatlng(datas[0], datas[1]);
                    activity.move();
                    break;
//                case 1:
//                    lat = Double.parseDouble(datas[0]);
//                    lng = Double.parseDouble(datas[1]);
//                    activity.setAimPoint(new LatLng(lat, lng));
//                    break;
//                case 2:
//                    double gyroAngle = Double.parseDouble(data);
//                    activity.setGyroAngle(gyroAngle);
//                    break;
//                case 3:
//                    double aimAngle = Double.parseDouble(data);
//                    activity.setAimAngle(aimAngle);
//                    break;
//                case 4:
//                    activity.setCurrVel(data);
//                    break;
//                case 5:
//                    activity.setGpsNum(data);
//                    break;
                case 7:
                    activity.handleState(Integer.parseInt(data));
                    break;
                default:
//                    Toast.makeText(context, "非法数据", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        catch (Exception e){
//            Toast.makeText(context, "非法数据", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}

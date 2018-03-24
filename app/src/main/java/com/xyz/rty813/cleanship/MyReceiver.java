package com.xyz.rty813.cleanship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;

public class MyReceiver extends BroadcastReceiver {
    public static final String ACTION_DATA_RECEIVED = "com.xyz.rty813.cleanship.ACTION_DATA_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        NewActivity activity = (NewActivity) context;
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            int current = intent.getExtras().getInt(BatteryManager.EXTRA_LEVEL);
            int total = intent.getExtras().getInt(BatteryManager.EXTRA_SCALE);
            int plugged = intent.getExtras().getInt(BatteryManager.EXTRA_PLUGGED);
//            Toast.makeText(context, String.format(Locale.CHINA, "%d\t%d\t%d", current, total, plugged), Toast.LENGTH_SHORT).show();
            if (plugged == 0) {
                if (current * 100 / total < 20) {
                    Toast.makeText(context, "电量低，请及时充电", Toast.LENGTH_LONG).show();
                } else if (current * 100 / total < 10) {
                    Toast.makeText(context, "电量过低！请立即返杭！", Toast.LENGTH_LONG).show();
                }
            }
        } else if (intent.getAction().equals(ACTION_DATA_RECEIVED)) {
            //        activity.setRawData(intent.getStringExtra("rawData"));
            String data = intent.getStringExtra("data");
            if (data == null) {
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
                        if ((lat < 0 || lat > 55 || lng < 70 || lng > 136)
                                || (Math.abs(lat - latLng.latitude) > 0.01) || (Math.abs(lng - latLng.longitude) > 0.01)) {
                            if (activity.getShipPointList().size() != 1) {
                                return;
                            }
                        }
                        activity.getShipPointList().add(new LatLng(lat, lng));
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
                    case 8:

                    default:
//                    Toast.makeText(context, "非法数据", Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
//            Toast.makeText(context, "非法数据", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

    }
}

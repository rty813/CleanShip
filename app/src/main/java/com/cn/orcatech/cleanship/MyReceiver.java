package com.cn.orcatech.cleanship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.cn.orcatech.cleanship.fragment.MapFragment;

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
                    Toasty.error(context, "电量过低！请立即返杭！", Toast.LENGTH_LONG).show();
                } else if (current * 100 / total < 20) {
                    Toasty.warning(context, "电量低，请及时充电", Toast.LENGTH_LONG).show();
                }
            }
        } else if (intent.getAction().equals(ACTION_DATA_RECEIVED)) {
            try {
                int shipid = intent.getIntExtra("shipid", -1);
                int state = Integer.parseInt(intent.getStringExtra("state"));
                int battery = Integer.parseInt(intent.getStringExtra("battery"));
                String[] latlng = intent.getStringExtra("latlng").split(",");
                double lat = Double.parseDouble(latlng[0]);
                double lng = Double.parseDouble(latlng[1]);
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
//                更新state
                if (state != mMapFragment.getShips().get(shipid).getPreState()) {
                    mMapFragment.getShips().get(shipid).setPreState(mMapFragment.getShips().get(shipid).getState());
                    mMapFragment.getShips().get(shipid).setState(state);
                    mMapFragment.updateShiplist(shipid, status);
                    if (shipid == activity.selectShip) {
                        mMapFragment.newHandleState(state);
                    }
                }

//                更新battery
                mMapFragment.getShips().get(shipid).setBattery(battery);
                mMapFragment.setBattery(battery);

//                更新坐标
                mMapFragment.getShips().get(shipid).setLat(lat);
                mMapFragment.getShips().get(shipid).setLng(lng);
                LatLng latLng = mMapFragment.getShipPointLists(shipid).get(mMapFragment.getShipPointLists(shipid).size() - 1);
                if ((lat < 0 || lat > 55 || lng < 70 || lng > 136)
                        || (Math.abs(lat - latLng.latitude) > 0.01) || (Math.abs(lng - latLng.longitude) > 0.01)) {
                    if (mMapFragment.getShipPointLists(shipid).size() != 1) {
                        return;
                    }
                }
                mMapFragment.getShipPointLists(shipid).add(new LatLng(lat, lng));
                mMapFragment.move(shipid);


            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
//        } else if (intent.getAction().equals(ACTION_DATA_RECEIVED)) {
//            String data = intent.getStringExtra("data");
//            if (data == null) {
//                return;
//            }
//            String[] datas = data.split(",");
////        0=>当前经纬度   1=>目标经纬度    2=>陀螺仪方向角   3=>目标方向角    4=>当前速度
//            try {
//                int type = Integer.parseInt(intent.getStringExtra("type"));
//                switch (type) {
//                    case 0:
//                        double lat = Double.parseDouble(datas[0]);
//                        double lng = Double.parseDouble(datas[1]);
//                        LatLng latLng = mMapFragment.getShipPointLists().get(mMapFragment.getShipPointLists().size() - 1);
//                        if ((lat < 0 || lat > 55 || lng < 70 || lng > 136)
//                                || (Math.abs(lat - latLng.latitude) > 0.01) || (Math.abs(lng - latLng.longitude) > 0.01)) {
//                            if (mMapFragment.getShipPointLists().size() != 1) {
//                                return;
//                            }
//                        }
//                        mMapFragment.getShipPointLists().add(new LatLng(lat, lng));
//                        mMapFragment.move();
//                        break;
//                    case 7:
//                        mMapFragment.handleState(Integer.parseInt(datas[0]));
//                        break;
//                    case 9:
//                        mMapFragment.setBattery(Integer.parseInt(data));
//                        break;
//                    default:
//                        break;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

    }
}

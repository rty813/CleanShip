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
        activity.getShipPointList().add(new LatLng(intent.getDoubleExtra("lat", 0),
                intent.getDoubleExtra("lng", 0)));
        activity.move();
    }
}

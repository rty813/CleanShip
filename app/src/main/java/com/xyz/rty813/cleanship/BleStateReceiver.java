package com.xyz.rty813.cleanship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.xyz.rty813.cleanship.ble.BluetoothLeService;

import es.dmoral.toasty.Toasty;

public class BleStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NewActivity activity = (NewActivity) context;
        switch (intent.getAction()) {
            case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                activity.onConnected();
                break;
            case BluetoothLeService.NPU_ACTION_GATT_DISCONNECTED:
                activity.mHandler.sendEmptyMessage(6);
                Toasty.error(context, "连接失败", Toast.LENGTH_SHORT).show();
                Log.e("BleStateReceiver", "onReceive: NPU_DISCONNECTED");
                break;
            case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                Log.e("BleStateReceiver", "onReceive: GATT_DISCONNECTED");
                break;
            case BluetoothLeService.ACTION_DATA_AVAILABLE:
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                break;
            default:
                break;
        }
    }
}

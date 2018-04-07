package com.xyz.rty813.cleanship.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.xyz.rty813.cleanship.NewActivity;

import es.dmoral.toasty.Toasty;

public class BleStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NewActivity activity = (NewActivity) context;
        switch (intent.getAction()) {
            case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                Log.e("BleStateReceiver", "onReceive: ONCONNECTED!!!!!!!!");
                activity.onConnected();
                break;
            case BluetoothLeService.NPU_ACTION_GATT_DISCONNECTED:
                activity.mHandler.sendEmptyMessage(3);
                Toasty.error(context, "连接失败", Toast.LENGTH_SHORT).show();
                Log.e("BleStateReceiver", "onReceive: NPU_DISCONNECTED");
                break;
            case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                Log.e("BleStateReceiver", "onReceive: GATT_DISCONNECTED");
                break;
            case BluetoothLeService.ACTION_DATA_AVAILABLE:
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                activity.onReceived(data);
                break;
            default:
                break;
        }
    }
}

package com.cn.orcatech.cleanship.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.cn.orcatech.cleanship.activity.NewActivity;

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
                Log.e("BleStateReceiver", "onReceive: NPU_DISCONNECTED");
                break;
            case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                Toasty.error(context, "本手机不支持该蓝牙模块！请联系开发者", Toast.LENGTH_LONG).show();
                activity.mHandler.sendEmptyMessage(3);
                Log.e("BleStateReceiver", "onReceive: GATT_DISCONNECTED");
                break;
            case BluetoothLeService.ACTION_DATA_AVAILABLE:
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                activity.onReceived(data);
                break;
            case NewActivity.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.STATE_OFF);
                Log.i("BLE_STATE", String.valueOf(state));
                if (state == BluetoothAdapter.STATE_ON) {
                    activity.initBleSerial();
                }
                if (state == BluetoothAdapter.STATE_OFF) {
                    activity.mHandler.sendEmptyMessage(3);
                }
                break;
            default:
                break;
        }
    }
}

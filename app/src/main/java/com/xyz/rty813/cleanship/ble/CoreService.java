package com.xyz.rty813.cleanship.ble;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;


/**
 * @author doufu
 */
public class CoreService extends Service {
    private final static String DEVICE_ADDRESS = "00:15:83:00:77:AF";
    private static BluetoothLeService bluetoothLeService;
    public boolean isConnected = false;
    private boolean isBindBleService = false;
    //BluetoothLeService回调
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            isBindBleService = true;
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            bluetoothLeService.initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            isBindBleService = false;
            isConnected = false;
            System.out.println("BLEService解绑");
        }
    };
    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        System.out.println(bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("CoreService被摧毁啦！onDestory");
        Toast.makeText(CoreService.this, "蓝牙连接已断开，核心服务停止！", Toast.LENGTH_SHORT).show();
        if (bluetoothLeService != null) {
            bluetoothLeService.close();
            bluetoothLeService = null;
        }
        if (isBindBleService) {
            unbindService(mServiceConnection);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public synchronized void writeData(String data, long delay) {
        if (isConnected) {
            bluetoothLeService.WriteValue(data);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void connect() {
        if (!isConnected) {
            bluetoothLeService.connect(DEVICE_ADDRESS);
        }
    }

    public class MyBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }
}

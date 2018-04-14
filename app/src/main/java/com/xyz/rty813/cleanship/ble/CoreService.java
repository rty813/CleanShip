package com.xyz.rty813.cleanship.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.xyz.rty813.cleanship.NewActivity;
import com.xyz.rty813.cleanship.R;


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
    private MyReceiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        System.out.println(bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.NPU_ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        mReceiver = new MyReceiver();
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("CoreService被摧毁啦！onDestory");
        showNotification(false);
        unregisterReceiver(mReceiver);
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
            try {
                if (data.length() > 20) {
                    String data1 = data.substring(0, 20);
                    String data2 = data.substring(20) + "\r\n";
                    bluetoothLeService.WriteValue(data1);
                    Thread.sleep(100);
                    bluetoothLeService.WriteValue(data2);
                } else {
                    bluetoothLeService.WriteValue(data + "\r\n");
                }
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

    public void close() {
        if (isConnected) {
            isConnected = false;
            bluetoothLeService.close();
            showNotification(true);
        }
    }

    public void showNotification(boolean enable) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("1", "正在运行", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(this, "1");
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(isConnected ? "已连接" : "未连接")
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, NewActivity.class), 0))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setTicker(isConnected ? "已连接" : "未连接")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();

        if (enable) {
            notificationManager.notify(666, notification);
        } else {
            notificationManager.cancel(666);
        }


    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothLeService.NPU_ACTION_GATT_DISCONNECTED:
                    close();
                    break;
                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    break;
                default:
                    break;
            }
        }
    }

    public class MyBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }
}

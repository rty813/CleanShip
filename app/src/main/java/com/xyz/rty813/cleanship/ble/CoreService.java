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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.xyz.rty813.cleanship.NewActivity;
import com.xyz.rty813.cleanship.R;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

import es.dmoral.toasty.Toasty;


/**
 * @author doufu
 */
public class CoreService extends Service {
    private final static String DEVICE_ADDRESS = "00:15:83:00:77:AF";
    private final static String ACTION_NOTIFICATION_CLOSE = "com.xyz.rty813.cleanship.ble.action.notification.close";
    private static BluetoothLeService bluetoothLeService;
    public boolean isConnected = false;
    public boolean notificationEnable = false;
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
    private MyReceiver mReceiver = null;
    private StringBuilder newData;
    private MediaPlayer mediaPlayer = null;
    private Vibrator vibrator = null;
    private ExecutorService queryThread;
    private boolean hasAlarmed = false;

    @Override
    public void onCreate() {
        super.onCreate();
        newData = new StringBuilder();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        System.out.println(bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
        queryThread = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onDestroy() {
        Toasty.warning(CoreService.this, "后台服务已关闭", Toast.LENGTH_SHORT).show();
        System.out.println("CoreService被摧毁啦！onDestory");
        startBackgroundThread(false);
        if (bluetoothLeService != null) {
            bluetoothLeService.close();
            bluetoothLeService = null;
        }
        if (isBindBleService) {
            unbindService(mServiceConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        System.out.println("CoreService被摧毁啦！onTaskRemoved");
        Toasty.info(CoreService.this, "后台服务已关闭", Toast.LENGTH_SHORT).show();
        startBackgroundThread(false);
        if (isBindBleService) {
            unbindService(mServiceConnection);
        }
        if (bluetoothLeService != null) {
            bluetoothLeService.close();
            bluetoothLeService = null;
        }
        super.onTaskRemoved(rootIntent);
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
                    Thread.sleep(1000);
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
        }
    }

    public void startBackgroundThread(boolean enable) {
        this.notificationEnable = enable;
        if (enable) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothLeService.NPU_ACTION_GATT_DISCONNECTED);
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
            intentFilter.addAction(ACTION_NOTIFICATION_CLOSE);
            mReceiver = new MyReceiver();
            registerReceiver(mReceiver, intentFilter);
            hasAlarmed = false;
            // 启动问询线程
            showNotification(true, -1);
            queryThread.execute(new QueryRunnable());
//            lowBtNotification();
        } else {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                mReceiver = null;
            }
            showNotification(false, null);
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
        }
    }

    public void showNotification(boolean enable, @Nullable Integer charge) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (enable) {
            NotificationCompat.Builder builder;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel("1", "正在运行", NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(notificationChannel);
                builder = new NotificationCompat.Builder(this, "1");
            } else {
                builder = new NotificationCompat.Builder(this);
            }
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
            remoteViews.setTextViewText(R.id.tv_charge,
                    String.format(Locale.getDefault(), "剩余电量：%d%%", charge));
            remoteViews.setOnClickPendingIntent(R.id.btn_close, PendingIntent.getBroadcast(this,
                    1, new Intent(ACTION_NOTIFICATION_CLOSE), PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setContentIntent(PendingIntent.getActivity(this, 2,
                    new Intent(this, NewActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContent(remoteViews)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true);
            Notification notification = builder.build();
            notificationManager.notify(666, notification);
        } else {
            notificationManager.cancelAll();
        }
    }

    private void lowBtNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("2", "低电量", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(this, "2");
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setContentIntent(PendingIntent.getActivity(this, 3,
                new Intent(this, NewActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle("蓝藻抑制机器人")
                .setContentText("低电量！")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();
        notificationManager.notify(6, notification);
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        // 震动
        if (vibrator == null) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        vibrator.vibrate(new long[]{1000, 1000}, 0);
        // 响铃
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        try {
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onReceived(String data) {
        newData.append(data);
    }

    @Nullable
    private String readData() {
        try {
            int timeGap = 0;
            String result = newData.toString();
            String oldResult = result;
            while (timeGap < 200 && !result.endsWith("#")) {
                timeGap = result.equals(oldResult) ? timeGap + 1 : 0;
                Thread.sleep(10);
                oldResult = result;
                result = newData.toString();
                if (!notificationEnable) {
                    break;
                }
            }
            newData = new StringBuilder();
            return result;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                    onReceived(data);
                    break;
                case BluetoothLeService.NPU_ACTION_GATT_DISCONNECTED:
                    Toasty.error(CoreService.this, "连接中断", Toast.LENGTH_SHORT).show();
                case ACTION_NOTIFICATION_CLOSE:
                    stopSelf();
                    break;
                default:
                    break;
            }
        }
    }

    private class QueryRunnable implements Runnable {

        @Override
        public void run() {
            while (notificationEnable) {
                try {
                    writeData(String.format(Locale.getDefault(), "$QUERY,9#"), 10);
                    String data = readData();
                    if (!notificationEnable) {
                        break;
                    }
                    Thread.sleep(100);
                    if (data.startsWith("$") && data.endsWith("#")) {
                        data = data.replaceAll("#", "");
                        data = data.replaceAll(Matcher.quoteReplacement("$"), "");
                        if (!"".equals(data)) {
                            String[] strings = data.split(";");
                            if (strings.length == 2) {
                                int charge = Integer.parseInt(strings[1]);
                                showNotification(true, charge);
                                if (charge < 20 && !hasAlarmed) {
                                    lowBtNotification();
                                    hasAlarmed = true;
                                }
                            }
                        }
                    }
                    for (int timeGap = 0; timeGap < 1000 && notificationEnable; timeGap++) {
                        Thread.sleep(10);
                    }
                } catch (NumberFormatException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class MyBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }
}

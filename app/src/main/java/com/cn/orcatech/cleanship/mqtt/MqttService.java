package com.cn.orcatech.cleanship.mqtt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.NewActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

import es.dmoral.toasty.Toasty;

public class MqttService extends Service {
    private final static String ACTION_NOTIFICATION_CLOSE = "com.xyz.rty813.cleanship.ble.action.notification.close";
    public boolean isConnected = false;
    public boolean notificationEnable = false;
    private MyBinder binder = new MyBinder();
    public StringBuilder newData;
    private MediaPlayer mediaPlayer = null;
    private Vibrator vibrator = null;
    private ExecutorService queryThread;
    private boolean hasAlarmed = false;
    private static final String TAG = "MQTT";
    private static final String TOPIC_SEND_PREFIX = "APP2SHIP";
    private static final String TOPIC_RECV_PREFIX = "SHIP2APP";
    public String TOPIC_SEND;
    public String TOPIC_RECV;
    public  String DEVICE_ID = null;
    private static final String MQTT_SERVER_URL = "tcp://orca-tech.cn:11883";
    public static final String MQTT_ONCONNCET = "MQTT_ONCONNECT";
    private MqttClient mqttClient;
    private MqttCallback mCallBack = new MqttCallback() {
        @Override
        public void messageArrived(String topic, MqttMessage message) {
            if (message != null) {
                Log.d(TAG, "messageArrived, topic: " + topic + "; message: " + new String(message.getPayload()));
                if (topic.equals(TOPIC_RECV)) {
                    onReceived(message.toString());
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "connectionLost");
            Toasty.error(MqttService.this, "连接中断！", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public IBinder onBind(Intent intent) { return binder; }
    
    @Override
    public void onCreate() {
        super.onCreate();
        newData = new StringBuilder();
        queryThread = Executors.newSingleThreadExecutor();
    }

    public void connect() {
        if (!isConnected) {
            try {
                mqttClient = new MqttClient(MQTT_SERVER_URL, "APP", null);
                mqttClient.setCallback(mCallBack);
                mqttClient.connect();
                mqttClient.subscribe(TOPIC_RECV);
                isConnected = true;
                sendBroadcast(new Intent(MQTT_ONCONNCET));
            } catch (MqttException e) {
                e.printStackTrace();
                Toasty.error(MqttService.this, "与MQTT服务器连接失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void close() {
        if(isConnected) {
            isConnected = false;
            try {
                mqttClient.disconnectForcibly();
                mqttClient.close(true);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        Toasty.warning(MqttService.this, "后台服务已关闭", Toast.LENGTH_SHORT).show();
        close();
        startBackgroundThread(false);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Toasty.info(MqttService.this, "后台服务已关闭", Toast.LENGTH_SHORT).show();
        close();
        startBackgroundThread(false);
        super.onTaskRemoved(rootIntent);
    }

    public void startBackgroundThread(boolean enable) {
        this.notificationEnable = enable;
        if (enable) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_NOTIFICATION_CLOSE);
            hasAlarmed = false;
            // 启动问询线程
            showNotification(true, -1);
            queryThread.execute(new QueryRunnable());
        } else {
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

    private void showNotification(boolean enable, @Nullable Integer charge) {
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

    synchronized public String readData(boolean mode) {
        try {
            int timeGap = 0;
            String result = newData.toString();
            String oldResult = result;
            while (timeGap < 200 && !result.endsWith("#")) {
                timeGap = result.equals(oldResult) ? timeGap + 1 : 0;
                Thread.sleep(10);
                oldResult = result;
                result = newData.toString();
                if (notificationEnable != mode) {
                    break;
                }
            }
            newData = new StringBuilder();
            return result;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

    synchronized public void writeData(String msg, long interval) {
        if (isConnected) {
            try {
                mqttClient.publish(TOPIC_SEND, (msg + "\r\n").getBytes(), 1, false);
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (MqttException e) {
                isConnected = false;
                e.printStackTrace();
            }
        }
    }

    public void setDeviceID(String id, int num) {
        DEVICE_ID = id;
        if (id != null) {
            TOPIC_SEND = String.format(Locale.getDefault(), "%s_%s_%d", TOPIC_SEND_PREFIX, id, num);
            TOPIC_RECV = String.format(Locale.getDefault(), "%s_%s_%d", TOPIC_RECV_PREFIX, id, num);
        }
    }

    private class QueryRunnable implements Runnable {

        @Override
        public void run() {
            while (notificationEnable) {
                try {
                    writeData(String.format(Locale.getDefault(), "$QUERY,9#"), 10);
                    String data = readData(true);
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
        public MqttService getService() {
            return MqttService.this;
        }
    }
}

package com.cn.orcatech.cleanship.mqtt;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.ble.CoreService;

import org.eclipse.paho.client.mqttv3.MqttException;

import es.dmoral.toasty.Toasty;


public class MqttActivity extends AppCompatActivity {

    private Button btnSend;
    private Button btnConnect;
    private TextView msgRecv;
    private MqttService mqttService;
    private MyHandler handler;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mqttService = ((MqttService.MyBinder) iBinder).getService();
            mqttService.startBackgroundThread(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("err", "disconnected");
        }
    };

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    msgRecv.setText((CharSequence) msg.obj);
                    Toasty.info(MqttActivity.this, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MqttService.MQTT_ONCONNCET:
                    onConnected();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt);
        initView();
        handler = new MyHandler();
        bindService(new Intent(this, MqttService.class), serviceConnection, BIND_AUTO_CREATE);
        registerReceiver(receiver, new IntentFilter(MqttService.MQTT_ONCONNCET));
    }

    @Override
    protected void onDestroy() {
        mqttService.close();
        super.onDestroy();
    }

    private void initView() {
        msgRecv = findViewById(R.id.tv_msgRecv);
        btnSend = findViewById(R.id.btn_send);
        btnConnect = findViewById(R.id.btn_connect);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mqttService.isConnected) {
                    try {
                        mqttService.writeData("$QUERY,0#", 10);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttService.connect();
            }
        });
    }

    public void onConnected() {
        Toasty.success(MqttActivity.this, "已连接", Toast.LENGTH_SHORT).show();
    }
}

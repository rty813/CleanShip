package com.cn.orcatech.cleanship.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.yanzhenjie.fragment.NoFragment;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class DataFragment extends NoFragment {
    private static Button btnTest;
    private static long time1;
    private static long time2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnTest = view.findViewById(R.id.btn_test);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    MqttClient mqttClient = ((MainActivity)getActivity()).getMapFragment().mqttClient;
                    mqttClient.subscribe("test1");
                    time1 = System.currentTimeMillis();
                    mqttClient.publish("test", String.valueOf(time1).getBytes(), 1, false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void setBtnText() {
        time2 = System.currentTimeMillis();
        btnTest.setText(String.valueOf(time2 - time1));
    }
}

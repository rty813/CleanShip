package com.xyz.rty813.cleanship;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.xyz.rty813.cleanship.util.SerialPortTool;

import java.io.IOException;

public class ControllerActivity extends AppCompatActivity implements View.OnClickListener {

    private SerialPortTool serialPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        serialPort = ConnectActivity.getSerialPort();
        findViewById(R.id.ctrl_1).setOnClickListener(this);
        findViewById(R.id.ctrl_2).setOnClickListener(this);
        findViewById(R.id.ctrl_3).setOnClickListener(this);
        findViewById(R.id.ctrl_4).setOnClickListener(this);
        findViewById(R.id.ctrl_5).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        try {
            switch (view.getId()) {
                case R.id.ctrl_1:
                    serialPort.writeData("1", 100);
                    break;
                case R.id.ctrl_2:
                    serialPort.writeData("2", 100);
                    break;
                case R.id.ctrl_3:
                    serialPort.writeData("3", 100);
                    break;
                case R.id.ctrl_4:
                    serialPort.writeData("4", 100);
                    break;
                case R.id.ctrl_5:
                    serialPort.writeData("5", 100);
                    break;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            Toast.makeText(ControllerActivity.this, "发送失败\n" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}

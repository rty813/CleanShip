package com.xyz.rty813.cleanship.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.xyz.rty813.cleanship.MainActivity;
import com.xyz.rty813.cleanship.MyReceiver;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by doufu on 2017/12/1.
 */

public class SerialPortTool {
    private PendingIntent mPermissionIntent;
    private UsbManager mUsbManager;
    private MainActivity mContext;
    private UsbSerialPort mPort;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int READY = 1;
    private static final int UNREADY = 0;
    private static final int GONE = 2;
    private UsbSerialDriver mDriver;
    private int mBaudRate;

    public SerialPortTool(Context context) {
        mContext = (MainActivity) context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        registerReceiver();
    }

    private void registerReceiver(){
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        mContext.registerReceiver(mUsbReceiver, filter);
    }

    public void unregisterReceiver(){
        mContext.unregisterReceiver(mUsbReceiver);
    }

    public List<UsbSerialDriver> searchSerialPort() {
        return UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
    }

    public void initDevice(UsbSerialDriver driver, int baudRate) {
        mDriver = driver;
        mBaudRate = baudRate;
        UsbDevice device = mDriver.getDevice();
        if (!mUsbManager.hasPermission(device)){
            mUsbManager.requestPermission(device, mPermissionIntent);
        }
        else{
            openDevice(device);
        }
    }

    public boolean openDevice(UsbDevice device){
        if(device != null){
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null) {
                mPort = mDriver.getPorts().get(0);
                try {
                    mPort.open(connection);
                    mPort.setParameters(mBaudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    System.out.println("已连接");
                    Toast.makeText(mContext, "已连接", Toast.LENGTH_SHORT).show();
                    mContext.morph(READY, 200);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (mContext.state != UNREADY){
                                String data = readData();
                                String[] strings = data.split(";");
                                System.out.println(data + "\t" + strings.length);
                                if (strings.length == 2){
                                    Intent intent = new Intent(MyReceiver.ACTION_DATA_RECEIVED);
                                    intent.putExtra("type", Integer.parseInt(strings[0]));
                                    intent.putExtra("data", strings[1]);
                                    mContext.sendBroadcast(intent);
                                }
                            }
                        }
                    }).start();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public void closeDevice() {
        try {
            if (mPort != null){
                mPort.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPort = null;
    }

    public void writeData(String data) throws IOException {
        mPort.write(data.getBytes(), 1000);
    }

    public String readData(){
        byte[] bytes = new byte[255];
        try {
            int len = mPort.read(bytes, 1000);
            return new String(bytes, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)){
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                        openDevice(device);
                    }else{
                        Log.d("Err", "permission denied for device " + device);
                    }
                }
            }
        }
    };
}

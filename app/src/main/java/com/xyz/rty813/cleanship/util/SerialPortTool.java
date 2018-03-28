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

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * Created by doufu on 2017/12/1.
 */

public class SerialPortTool{
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private UsbManager mUsbManager;
//    private ConnectActivity mContext;
//    private MapActivity mContext;
    private Context mContext;
    private UsbSerialPort mPort;
    private UsbSerialDriver mDriver;
    private int mBaudRate;
    private onConnectedListener mListener;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        try {
                            openDevice(device);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("Err", "permission denied for device " + device);
                    }
                }
            }
        }
    };

    public SerialPortTool(Context context) {
//        mContext = (ConnectActivity) context;
        mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
//        registerReceiver();
    }

    public void setListener(onConnectedListener listener) {
        this.mListener = listener;
    }

    public void registerReceiver(Context context){
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
    }

    public void unregisterReceiver(Context context){
        context.unregisterReceiver(mUsbReceiver);
    }

    public List<UsbSerialDriver> searchSerialPort() {
        return UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
    }

    public void initDevice(UsbSerialDriver driver, int baudRate) throws IOException {
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

    private void openDevice(UsbDevice device) throws IOException {
        if(device != null){
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null) {
                mPort = mDriver.getPorts().get(0);
                mPort.open(connection);
                mPort.setParameters(mBaudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                if (mListener != null) {
                    mListener.onConnected();
                }
            }
            else
                throw new IOException();
        }
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

    public synchronized void writeByte(byte[] data, int delay) throws IOException, InterruptedException {
        if (mPort != null){
            mPort.write(data, 1000);
            if (delay != 0){
                Thread.sleep(delay);
            }
        }
    }

    public synchronized void writeData(String data, long delay) throws IOException, InterruptedException {
        if (data.equals("")){
            return;
        }
        writeData(data, delay, true);
    }

    public synchronized void writeData(String data, long delay, boolean newLine) throws InterruptedException, IOException {
        String str = data;
        if (newLine){
            str = str + "\r\n";
        }
        if (mPort != null){
            mPort.write(str.getBytes(), 1000);
        }
        Thread.sleep(delay);
    }

    public synchronized String readData(){
        byte[] bytes = new byte[255];
        if (mPort == null){
            return "";
        }
        try {
            int len = mPort.read(bytes, 2000);
            return new String(bytes, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public interface onConnectedListener {
        void onConnected();
    }
}

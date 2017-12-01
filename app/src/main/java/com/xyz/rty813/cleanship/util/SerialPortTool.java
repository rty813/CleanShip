package com.xyz.rty813.cleanship.util;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * Created by doufu on 2017/12/1.
 */

public class SerialPortTool {
    private UsbManager mUsbManager;
    private Context mContext;
    private UsbSerialPort mPort;
    private UsbDeviceConnection mConnection;
    public SerialPortTool(Context context) {
        mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    }

    public List<UsbSerialDriver> searchSerialPort() {
        return UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
    }

    public void initDevice(UsbSerialDriver driver) throws IOException {
        mConnection = mUsbManager.openDevice(driver.getDevice());
        if (mConnection == null) {
            throw new IOException("Err: Connection is Null");
        }
        mPort = driver.getPorts().get(0);

    }

    public void openDevice(int baudRate) throws IOException {
        mPort.open(mConnection);
        mPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
    }

    public void closeDevice() throws IOException {
        mPort.close();
    }

    public void writeData(){

    }

    public void readData(){

    }
}

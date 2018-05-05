package com.cn.orcatech.cleanship;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;

import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.utils.FileUtils;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author doufu
 */
public class RemoteControlActivity extends AppCompatActivity {
    private CameraViewInterface mUVCCameraView;
    private boolean isRequest = false;
    private boolean isPreview = false;
    private UVCCameraHelper mCameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);
        mUVCCameraView = findViewById(R.id.textureview);
        mUVCCameraView.setCallback(mCallback);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }
    private CameraViewInterface.Callback mCallback = new CameraViewInterface.Callback() {
        @Override
        public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
            // must have
            if (!isPreview && mCameraHelper.isCameraOpened()) {
                mCameraHelper.startPreview(mUVCCameraView);
                isPreview = true;
            }
        }

        @Override
        public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {}

        @Override
        public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
            // must have
            if (isPreview && mCameraHelper.isCameraOpened()) {
                mCameraHelper.stopPreview();
                isPreview = false;
            }
        }
    };

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {
        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission(must have)
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    USBMonitor mUSBMonitor = mCameraHelper.getUSBMonitor();
                    if(mUSBMonitor != null) {
                        mUSBMonitor.requestPermission(device);
                    }
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera(must have)
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {}

        @Override
        public void onDisConnectDev(UsbDevice device) {}
    };
}
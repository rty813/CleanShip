package com.cn.orcatech.cleanship.activity;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.utils.FileUtils;
import com.kongqw.rockerlibrary.view.RockerView;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.io.File;

/**
 * @author doufu
 */
public class RemoteControlActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "RemoteControlActivity";
    private String ROOT_PATH;
    private CameraViewInterface mUVCCameraView;
    private boolean isRequest = false;
    private boolean isPreview = false;
    private UVCCameraHelper mCameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);
        ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/欧卡/";
        mUVCCameraView = findViewById(R.id.textureview);
        mUVCCameraView.setCallback(mCallback);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);
        findViewById(R.id.btn_capture).setOnClickListener(this);
        findViewById(R.id.btn_record).setOnClickListener(this);
        RockerView rockerView = findViewById(R.id.rockerview);
        rockerView.setCallBackMode(RockerView.CallBackMode.CALL_BACK_MODE_MOVE);
        rockerView.setOnAngleChangeListener(new RockerView.OnAngleChangeListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void angle(double v) {
                Log.i(TAG, "angle: " + v);
            }

            @Override
            public void onFinish() {

            }
        });
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

    @Override
    public void onClick(View view) {
        File file = new File(ROOT_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        switch (view.getId()) {
            case R.id.btn_capture:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    Toast.makeText(RemoteControlActivity.this, "sorry,camera open failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                String picPath = ROOT_PATH + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;

                mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        showToast("已截图");
                        Log.i("SAVE", "save path：" + path);
                    }
                });
                break;
            case R.id.btn_record:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    Toast.makeText(RemoteControlActivity.this, "sorry,camera open failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mCameraHelper.isRecording()) {
                    String videoPath = ROOT_PATH + System.currentTimeMillis();
                    RecordParams params = new RecordParams();
                    params.setRecordPath(videoPath);
                    // 设置为0，不分割保存
                    params.setRecordDuration(0);
                    params.setVoiceClose(true);
                    mCameraHelper.startRecording(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                        @Override
                        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                        }

                        @Override
                        public void onRecordResult(String videoPath) {
                            Log.i("Video", "videoPath = " + videoPath);
                        }
                    });
                    Toast.makeText(RemoteControlActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
                } else {
//                    FileUtils.releaseFile();
                    mCameraHelper.stopRecording();
                    Toast.makeText(this, "结束录制", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RemoteControlActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
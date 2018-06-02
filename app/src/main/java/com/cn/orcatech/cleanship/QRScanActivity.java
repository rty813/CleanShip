package com.cn.orcatech.cleanship;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import es.dmoral.toasty.Toasty;

/**
 * @author doufu
 */
public class QRScanActivity extends AppCompatActivity implements QRCodeView.Delegate {

    private static final String TAG = "QRScan";
    private QRCodeView mQRCodeView;
    public static final String DEVICE_INFO = "DEVICE_INFO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);
        mQRCodeView = findViewById(R.id.zxingview);
        mQRCodeView.setDelegate(this);
        AndPermission.with(this)
                .permission(Permission.CAMERA, Permission.READ_EXTERNAL_STORAGE)
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        Toast.makeText(QRScanActivity.this, "需要权限", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        mQRCodeView.startCamera();
                        mQRCodeView.showScanRect();
                        mQRCodeView.startSpot();
                    }
                })
                .start();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        mQRCodeView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mQRCodeView.onDestroy();
        super.onDestroy();
    }


    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }



    @Override
    public void onScanQRCodeSuccess(String result) {
        Log.e(TAG, "onScanQRCodeSuccess: " + result);
        mQRCodeView.startSpot();
        String[] datas = result.split(";");
        if (datas.length < 3) {
            return;
        }
        String id = datas[0];
        String name = datas[1];
        String addr = datas[2];
        String pattern = "\\w+:\\w+:\\w+:\\w+:\\w+:\\w+";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(addr);
        if (m.matches()) {
            Toasty.success(this, name + "连接成功！", Toast.LENGTH_SHORT).show();
            vibrate();
            SharedPreferences sharedPreferences = getSharedPreferences(DEVICE_INFO, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("addr", addr);
            editor.putString("name", name);
            editor.putString("id", id);
            editor.apply();
            finish();
            startActivity(new Intent(this, NewActivity.class));
        }
        else {
            return;
        }
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e(TAG, "打开相机出错");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}

package com.xyz.rty813.cleanship;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.model.MyLocationStyle;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.kcode.lib.UpdateWrapper;
import com.kcode.lib.bean.VersionModel;
import com.kcode.lib.net.CheckUpdateTask;
import com.ldoublem.loadingviewlib.view.LVCircularSmile;
import com.ldoublem.loadingviewlib.view.LVRingProgress;
import com.xyz.rty813.cleanship.util.SerialPortTool;
import com.yanzhenjie.nohttp.NoHttp;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.download.DownloadRequest;
import com.yanzhenjie.nohttp.download.SimpleDownloadListener;
import com.yanzhenjie.nohttp.download.SyncDownloadExecutor;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;

public class ConnectActivity extends AppCompatActivity implements SerialPortTool.onConnectedListener {

    private static SerialPortTool serialPort;
    private LVRingProgress loadingView;
    private TextView tv_connect;
    private TextView tv_welcome;
    private ProgressDialog progressDialog;
    private int rfVersion = 0;
    private String rfSha256 = "";
    private String rfName = "";
    private MyHandler mHandler = null;

    private static final int READY = 1;
    private static final int UNREADY = 0;
    public static int state = UNREADY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        initView();
        requestPermission();
        NoHttp.initialize(this);
        serialPort = new SerialPortTool(this);
        serialPort.setListener(this);
        mHandler = new MyHandler();
    }

    private void requestPermission() {
        AndPermission.with(this)
                .permission(Permission.ACCESS_COARSE_LOCATION, Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        checkUpdate();
                        new Thread(new GetRFInfo()).start();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        final SettingService settingService = AndPermission.permissionSetting(ConnectActivity.this);
                        new AlertDialog.Builder(ConnectActivity.this)
                                .setMessage("请赋予权限")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        settingService.execute();
                                        finish();
                                    }
                                })
                                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        settingService.cancel();
                                        finish();
                                    }
                                })
                                .show();
                    }
                })
                .rationale(new Rationale() {
                    @Override
                    public void showRationale(Context context, List<String> permissions, final RequestExecutor executor) {
                        final PopupWindow popupWindow = new PopupWindow();
                        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                        View contentView = LayoutInflater.from(ConnectActivity.this).inflate(R.layout.permission_ask, null);
                        contentView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                popupWindow.dismiss();
                                executor.execute();
                            }
                        });
                        popupWindow.setContentView(contentView);
                        popupWindow.setAnimationStyle(R.style.dismiss_anim);
                        tv_connect.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!ConnectActivity.this.isFinishing()){
                                    popupWindow.showAtLocation(ConnectActivity.this.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                                }
                            }
                        });
                    }
                })
                .start();
    }

    private void initView() {
        loadingView = findViewById(R.id.loading_view);
        loadingView.setViewColor(R.color.colorBlue);
        tv_connect = findViewById(R.id.tv_connect);
        tv_welcome = findViewById(R.id.tv_welcome);
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingView.setVisibility(View.VISIBLE);
                tv_connect.setVisibility(View.VISIBLE);
                tv_connect.setText("正在连接");
                loadingView.startAnim(2000);
                initSerialPort(115200);
            }
        });
    }

    private void initSerialPort(int baudRate){
        System.out.println("init serial port");
        List<UsbSerialDriver> list = serialPort.searchSerialPort();
        if (list.isEmpty()){
            unConnected("未连接设备");
        }else{
            try {
                serialPort.initDevice(list.get(0), baudRate);
            } catch (IOException e) {
                e.printStackTrace();
                unConnected("连接失败");
            }
        }
    }

    private void unConnected(String msg){
        state = UNREADY;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        loadingView.stopAnim();
        tv_connect.setText("未连接");
        tv_welcome.setVisibility(View.INVISIBLE);
        loadingView.setVisibility(View.INVISIBLE);
    }

    public static SerialPortTool getSerialPort() {
        return serialPort;
    }

    public static int getState() {
        return state;
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 3:
                    serialPort.closeDevice();
                    unConnected("连接中断");
                    break;
                case 9:
                    synchronized (serialPort){
                        Toast.makeText(ConnectActivity.this, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        state = READY;
                        serialPort.notify();
                        startActivity(new Intent(ConnectActivity.this, MapActivity.class));
                        finish();
                    }
                    break;
                case 10:
                    progressDialog.show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onConnected() {
        tv_connect.setText("已连接");
        tv_welcome.setVisibility(View.VISIBLE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("更新固件中");
        new Thread(new CheckFirmwareUpdate()).start();
    }

    private class CheckFirmwareUpdate implements Runnable {
        @Override
        public void run() {
            try {
                serialPort.writeData("", 10);
                int lfVersion = Integer.parseInt(serialPort.readData()
                        .replace("\n", "")
                        .replace("\r", ""));
                if (lfVersion < rfVersion) {
                    String url = "http://rty813.xyz/cleanship/bin/" + rfName;
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cleanship/";
                    DownloadRequest request = new DownloadRequest(url, RequestMethod.GET, path, true, true);
                    SyncDownloadExecutor.INSTANCE.execute(0, request, new SimpleDownloadListener() {
                        @Override
                        public void onFinish(int what, String filePath) {
                            try {
                                InputStream inputStream = new FileInputStream(filePath);
                                int size = inputStream.available();
                                byte[] buffer = new byte[size];
                                inputStream.read(buffer);
                                inputStream.close();
                                // 计算SHA-256 校验
                                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                                digest.update(buffer);
                                BigInteger bigInteger = new BigInteger(1, digest.digest());
                                Message msg = mHandler.obtainMessage(9);
                                if (!bigInteger.toString(16).equals(rfSha256)) {
                                    msg.obj = "校验错误！";
                                    mHandler.sendMessage(msg);
                                    return;
                                }
                                serialPort.writeData("y", 1000);
                                serialPort.writeByte(buffer, 0);
                                final String response = serialPort.readData();
                                if (response.contains("Success")) {
                                    msg.obj = "固件更新成功";
                                } else {
                                    msg.obj = "固件更新失败";
                                }
                                mHandler.sendMessage(msg);
                            } catch (IOException e) {
                                e.printStackTrace();
                                mHandler.sendEmptyMessage(3);
                            } catch (Exception e) {
                                Message msg = mHandler.obtainMessage(9);
                                msg.obj = "固件更新失败";
                                mHandler.sendMessage(msg);
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    synchronized (serialPort) {
                        Message msg = mHandler.obtainMessage(9);
                        msg.obj = "已是最新版";
                        mHandler.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(3);
            } catch (InterruptedException|NumberFormatException e) {
                e.printStackTrace();
                Message msg = mHandler.obtainMessage(9);
                msg.obj = "固件更新失败";
                mHandler.sendMessage(msg);
            }
        }
    }

    private class GetRFInfo implements Runnable{
        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("http://139.199.37.92/cleanship/bin.json").openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                final JSONObject json = new JSONObject(builder.toString());
                rfName = json.getString("name");
                rfVersion = json.getInt("version");
                rfSha256 = json.getString("sha256");
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void checkUpdate() {
        System.out.println("check update");
        UpdateWrapper updateWrapper = new UpdateWrapper.Builder(getApplicationContext())
                .setTime(1000)
                .setNotificationIcon(R.mipmap.ic_launcher)
                .setUrl("http://rty813.xyz/cleanship/app.json")
                .setIsShowToast(false)
                .setCallback(new CheckUpdateTask.Callback() {
                    @Override
                    public void callBack(VersionModel versionModel) {
                        Log.d("Update","new version :" + versionModel.getVersionCode() + ";version info" + versionModel.getVersionName());
                    }
                })
                .build();
        updateWrapper.start();
    }
}

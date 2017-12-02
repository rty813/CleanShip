package com.xyz.rty813.cleanship;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class SerialPortActivity extends AppCompatActivity {
    private ImageView iv_canvas;
    private Bitmap baseBitmap;
    private Canvas canvas;
    private Paint paint;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_port);

        paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setColor(Color.RED);

        iv_canvas = findViewById(R.id.view);
        iv_canvas.setOnTouchListener(new View.OnTouchListener() {
            float startX;
            float startY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    // 用户按下动作
                    case MotionEvent.ACTION_DOWN:
                        // 第一次绘图初始化内存图片，指定背景为白色
                        if (baseBitmap == null) {
                            baseBitmap = Bitmap.createBitmap(iv_canvas.getWidth(),
                                    iv_canvas.getHeight(), Bitmap.Config.ARGB_8888);
                            canvas = new Canvas(baseBitmap);
                            canvas.drawColor(Color.WHITE);
                        }
                        // 记录开始触摸的点的坐标
                        startX = motionEvent.getX();
                        startY = motionEvent.getY();
                        break;
                    // 用户手指在屏幕上移动的动作
                    case MotionEvent.ACTION_MOVE:
                        // 记录移动位置的点的坐标
                        float stopX = motionEvent.getX();
                        float stopY = motionEvent.getY();

                        //根据两点坐标，绘制连线
                        canvas.drawLine(startX, startY, stopX, stopY, paint);

                        // 更新开始点的位置
                        startX = motionEvent.getX();
                        startY = motionEvent.getY();

                        // 把图片展示到ImageView中
                        iv_canvas.setImageBitmap(baseBitmap);
                        break;
                    case MotionEvent.ACTION_UP:

                        break;
                    default:
                        break;
                }
                return true;
            }
        });

/*********************************************************************************************************/

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()){
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (UsbSerialDriver driver : availableDrivers){
            System.out.println(driver.getDevice());
            builder.append(driver.getDevice()).append("\n");
        }
        ((TextView)findViewById(R.id.tv_serial)).setText(builder.toString());

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null){
            return;
        }
        UsbSerialPort port = driver.getPorts().get(0);
        try{
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            byte buffer[] = new byte[16];
            int numBytesRead = port.read(buffer, 1000);
            Log.d("PORT::", "Read " + numBytesRead + " bytes.");
            port.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

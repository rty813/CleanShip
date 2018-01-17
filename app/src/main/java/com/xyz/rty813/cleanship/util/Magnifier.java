package com.xyz.rty813.cleanship.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Created by doufu on 2018-01-17.
 */

public class Magnifier extends View {
    private Paint mPaint;
    public static final int WIDTH = 50;
    public static final int HEIGHT = 50;

    public Magnifier(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(0xffff0000);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        // draw popup
        mPaint.setAlpha(255);
//        canvas.drawBitmap(resBitmap, 0, 0, mPaint);
        canvas.restore();

        //draw popup frame
        mPaint.reset();//重置
        mPaint.setColor(Color.LTGRAY);
        mPaint.setStyle(Paint.Style.STROKE);//设置空心
        mPaint.setStrokeWidth(2);
        Path path1 = new Path();
        path1.moveTo(0, 0);
        path1.lineTo(WIDTH, 0);
        path1.lineTo(WIDTH, HEIGHT);
        path1.lineTo(WIDTH / 2 + 15, HEIGHT);
        path1.lineTo(WIDTH / 2, HEIGHT + 10);
        path1.lineTo(WIDTH / 2 - 15, HEIGHT);
        path1.lineTo(0, HEIGHT);
        path1.close();//封闭
        canvas.drawPath(path1, mPaint);
    }

}

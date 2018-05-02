package com.cn.orcatech.cleanship;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.cn.orcatech.cleanship.util.LayoutGravity;

/**
 * Created by doufu on 2017/12/2.
 */

public abstract class MyPopupWindow {
    protected Context context;
    protected View contentView;
    protected PopupWindow mInstance;

    public MyPopupWindow(Context c, int layoutRes, int w, int h) {
        context=c;
        contentView= LayoutInflater.from(c).inflate(layoutRes, null, false);
        initView();
        initEvent();
        mInstance=new PopupWindow(contentView, w, h, true);
        initWindow();
    }

    public View getContentView() { return contentView; }
    public PopupWindow getPopupWindow() { return mInstance; }

    protected abstract void initView();
    protected abstract void initEvent();

    protected void initWindow() {
        mInstance.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mInstance.setOutsideTouchable(true);
        mInstance.setTouchable(true);
    }

    public void showBashOfAnchor(View anchor, LayoutGravity layoutGravity, int xmerge, int ymerge) {
        int[] offset=layoutGravity.getOffset(anchor, mInstance);
        mInstance.showAsDropDown(anchor, offset[0]+xmerge, offset[1]+ymerge);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff) {
        mInstance.showAsDropDown(anchor, xoff, yoff);
    }

    public void showAtLocation(View parent, int gravity, int x, int y) {
        mInstance.showAtLocation(parent, gravity, x, y);
    }

}

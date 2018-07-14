package com.cn.orcatech.cleanship;

public class Ship {
    private double lat = 0;
    private double lng = 0;
    private int battery = 0;

    // state 正数=循环模式第几圈且正在运行 0=刚上电啥都没干 -1=连线模式运行中 -2=循环模式暂停 -3=连线模式暂停 -4=连线模式结束 -5=返航 -10=待机 -11=关机
    private int state = -11;
    private int turns = 0;

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getStatus() {
        if (state == -10) {
            return 0;
        }
        else if (state == -11) {
            return -1;
        }
        else {
            return 1;
        }
    }
}

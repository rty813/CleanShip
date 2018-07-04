package com.cn.orcatech.cleanship;

public class Ship {
    private double lat = 0;
    private double lng = 0;
    private int battery = 0;
    private int state = -11;
    private int preState = -11;

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

    public int getPreState() {
        return preState;
    }

    public void setPreState(int preState) {
        this.preState = preState;
    }
}

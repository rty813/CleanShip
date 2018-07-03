package com.cn.orcatech.cleanship;

public class Ship {
    private int states = -1;
    private double lat;
    private double lng;
    private int battery;

    public int getStatus() {
        return states;
    }

    public void setStates(int states) {
        this.states = states;
    }

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
}

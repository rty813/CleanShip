package com.cn.orcatech.cleanship;

public class Ship {
    private String states = "离线";
    private double lat;
    private double lng;
    private int battery;

    public String getStatus() {
        return states;
    }

    public void setStates(String states) {
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

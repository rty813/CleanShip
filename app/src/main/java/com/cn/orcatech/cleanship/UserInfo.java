package com.cn.orcatech.cleanship;

public class UserInfo {
    private String username;
    private String password;
    private String icon;
    private int ship_id;
    private int totalship;

    public String getIcon() {
        return icon;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public int getShip_id() {
        return ship_id;
    }

    public int getTotalship() {
        return totalship;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setShip_id(int ship_id) {
        this.ship_id = ship_id;
    }

    public void setTotalship(int totalship) {
        this.totalship = totalship;
    }
}

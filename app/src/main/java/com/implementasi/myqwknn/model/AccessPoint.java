package com.implementasi.myqwknn.model;

public class AccessPoint {
    private String bssid;
    private String ssid;
    private double rss;

    public AccessPoint(String bssid, String ssid, double rss) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.rss = rss;
    }

    public String getBssid() { return bssid; }
    public String getSsid() { return ssid; }
    public double getRss() { return rss; }

    public void setBssid(String bssid) { this.bssid = bssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public void setRss(double rss) { this.rss = rss; }
}
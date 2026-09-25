package com.implementasi.myqwknn.model;

import java.util.HashMap;
import java.util.Map;

public class ReferencePoint {
    private int id;
    private String roomName;
    private double x;
    private double y;
    private Map<String, Double> rssMap; // key: bssid, value: mean RSS

    public ReferencePoint(int id, String roomName, double x, double y) {
        this.id = id;
        this.roomName = roomName;
        this.x = x;
        this.y = y;
        this.rssMap = new HashMap<>();
    }

    public int getId() { return id; }
    public String getRoomName() { return roomName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Map<String, Double> getRssMap() { return rssMap; }

    public void setId(int id) { this.id = id; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setRssMap(Map<String, Double> rssMap) { this.rssMap = rssMap; }

    public void addRss(String bssid, double meanRss) {
        this.rssMap.put(bssid, meanRss);
    }
}
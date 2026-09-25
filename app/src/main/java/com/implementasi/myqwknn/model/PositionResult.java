package com.implementasi.myqwknn.model;

public class PositionResult {
    private double x;
    private double y;
    private String roomName;
    private int kAdaptive;
    private long computationTimeMs;
    private String algorithmName;
    private double errorDistance;
    private String neighborDetails; // detail tetangga terpilih untuk fitur debug

    public PositionResult(double x, double y, String roomName,
                          int kAdaptive, long computationTimeMs,
                          String algorithmName) {
        this.x = x;
        this.y = y;
        this.roomName = roomName;
        this.kAdaptive = kAdaptive;
        this.computationTimeMs = computationTimeMs;
        this.algorithmName = algorithmName;
        this.errorDistance = -1; // belum diisi
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getRoomName() { return roomName; }
    public int getKAdaptive() { return kAdaptive; }
    public long getComputationTimeMs() { return computationTimeMs; }
    public String getAlgorithmName() { return algorithmName; }
    public double getErrorDistance() { return errorDistance; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setKAdaptive(int kAdaptive) { this.kAdaptive = kAdaptive; }
    public void setComputationTimeMs(long computationTimeMs) { this.computationTimeMs = computationTimeMs; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    public void setErrorDistance(double errorDistance) { this.errorDistance = errorDistance; }
    public String getNeighborDetails() { return neighborDetails; }
    public void setNeighborDetails(String neighborDetails) { this.neighborDetails = neighborDetails; }

    @Override
    public String toString() {
        return algorithmName + " → Room: " + roomName +
                " | X: " + String.format("%.2f", x) +
                " | Y: " + String.format("%.2f", y) +
                " | K: " + kAdaptive +
                " | Time: " + computationTimeMs + "ms";
    }
}
package io.github.borngle.homehelper.model;

public class SensorNode {
    private String room;
    private String url;
    private float temperature;
    private float humidity;
    private float lux;
    private boolean motion;
    private boolean reachable; // Failed requests

    public SensorNode(String room, String url) {
        this.room = room;
        this.url = url;
    }

    public String getRoom() {
        return room;
    }

    public String getUrl() {
        return url;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public float getLux() {
        return lux;
    }

    public boolean isMotion() {
        return motion;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

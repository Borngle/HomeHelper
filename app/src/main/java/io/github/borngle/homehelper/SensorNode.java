package io.github.borngle.homehelper;

// Model class
public class SensorNode {
    private String room;
    private String url;
    private float temperature;
    private float humidity;
    private float lux;
    private boolean motion;
    private boolean reachable; // Failed requests

    // Notifications
    private boolean notifyHeating = true;
    private boolean notifyHumidity = true;
    private boolean notifyLights = true;

    public SensorNode(String room, String url) {
        this.room = room;
        this.url = url;
    }

    public String getRoom() {
        return room;
    }

    public String getUrl() {
        if (this.url == null || this.url.isEmpty()) {
            return null;
        }
        return this.url;
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

    public boolean getNotifyHeating() {
        return notifyHeating;
    }
    public boolean getNotifyHumidity() {
        return notifyHumidity;
    }
    public boolean getNotifyLights() {
        return notifyLights;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public void setLux(float lux) {
        this.lux = lux;
    }

    public void setMotion(boolean motion) {
        this.motion = motion;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public void setNotifyHeating(boolean notifyHeating) {
        this.notifyHeating = notifyHeating;
    }

    public void setNotifyHumidity(boolean notifyHumidity) {
        this.notifyHumidity = notifyHumidity;
    }

    public void setNotifyLights(boolean notifyLights) {
        this.notifyLights = notifyLights;
    }
}

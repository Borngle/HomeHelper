package io.github.borngle.homehelper;

import java.util.Calendar;

public class RoomAnalyser {
    // Thresholds

    private static final float occupiedMotionFraction = 0.15f; // 15% of polls had motion
    private static final float temperatureDelta = 0.5f; // Rise over the recording window
    private static final float temperatureAboveOutside = 3; // Room warmer than outside by this much
    private static final float moderateOutsideTemperature = 15;
    private static final float warmInsideTemperature = 25;

    private static final float highHumidity = 70; // Uncomfortable above this
    private static final float lowHumidity = 30; // Uncomfortable below this
    private static final float humidityDelta = 10; // Rise over the recording window

    private static final float lightsOnLux = 85; // Lux above this means lights likely on
    private static final float darkRoomLux = 25; // Lux below this means room is dark
    private static final float lightsDelta = 25; // Jump within recording suggests lights likely on
    private static final float darkOutsideLux = 20;
    private static final float brightOutsideLux = 5000;

    public static class Analysis {
        public final boolean likelyOccupied;
        // Heating
        public final boolean heatingLikelyOn;
        public final boolean roomCold;
        public final boolean roomWarm;
        public final boolean heatingUnnecessary;
        // Humidity
        public final boolean humidityHigh;
        public final boolean humidityLow;
        // Lights
        public final boolean lightsLikelyOn;
        public final boolean roomDarkDuringDay;
        public final boolean lightsUnnecessary;
        public final boolean obstructed;

        public Analysis(boolean likelyOccupied, boolean heatingLikelyOn, boolean roomCold, boolean roomWarm,
                        boolean heatingUnnecessary, boolean humidityHigh, boolean humidityLow, boolean lightsLikelyOn,
                        boolean roomDarkDuringDay, boolean lightsUnnecessary, boolean obstructed) {
            this.likelyOccupied = likelyOccupied;
            this.heatingLikelyOn = heatingLikelyOn;
            this.roomCold = roomCold;
            this.roomWarm = roomWarm;
            this.heatingUnnecessary = heatingUnnecessary;
            this.humidityHigh = humidityHigh;
            this.humidityLow = humidityLow;
            this.lightsLikelyOn = lightsLikelyOn;
            this.roomDarkDuringDay = roomDarkDuringDay;
            this.lightsUnnecessary = lightsUnnecessary;
            this.obstructed = obstructed;
        }
    }

    public Analysis analyse(SensorNode sensorNode, SensorNodeHistory sensorNodeHistory, float outsideTemperature, float outsideLux) {
        // Occupancy
        boolean occupied = sensorNodeHistory.motionFraction() >= occupiedMotionFraction;
        // Heating
        float gap = sensorNode.getTemperature() - outsideTemperature;
        float temperatureTrend = sensorNodeHistory.temperatureTrend();
        boolean heatingOn = gap > temperatureAboveOutside && temperatureTrend > temperatureDelta;
        boolean roomCold = sensorNode.getTemperature() < moderateOutsideTemperature && sensorNodeHistory.temperatureTrend() < 0;
        boolean roomWarm = sensorNode.getTemperature() > warmInsideTemperature && sensorNodeHistory.temperatureTrend() > 0;
        boolean heatingUnnecessary = heatingOn && outsideTemperature > moderateOutsideTemperature;
        // Humidity
        boolean humidityHigh = sensorNodeHistory.averageHumidity() >= highHumidity || sensorNodeHistory.humidityTrend() > humidityDelta;
        boolean humidityLow = sensorNodeHistory.averageHumidity() <= lowHumidity; // Air is too dry
        // Lights
        float roomLux = sensorNodeHistory.averageLux();
        float currentLux = sensorNode.getLux();
        float luxTrend = sensorNodeHistory.luxTrend();
        boolean isDarkOutside = outsideLux <= darkOutsideLux;
        boolean isBrightOutside = outsideLux >= brightOutsideLux;
        boolean lightsOn = (currentLux > lightsOnLux) && luxTrend >= lightsDelta; // Room fairly bright and trend is a spike
        boolean roomDarkDuringDay = isBrightOutside && roomLux < darkRoomLux && occupied;
        boolean lightsUnnecessary = lightsOn && isBrightOutside;
        boolean obstructed = sensorNodeHistory.averageLux() <= 0 && !isDarkOutside
                && sensorNodeHistory.getRecordings().size() >= 10;
        return new Analysis(occupied, heatingOn, roomCold, roomWarm, heatingUnnecessary, humidityHigh,
                humidityLow, lightsOn, roomDarkDuringDay, lightsUnnecessary, obstructed);
    }
}

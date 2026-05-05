package io.github.borngle.homehelper;

import java.util.Calendar;

public class RoomAnalyser {
    // Thresholds
    private static final float occupiedMotionFraction = 0.15f; // 15% of polls had motion
    private static final float temperatureDelta = 0.5f; // Rise over the recording window
    private static final float temperatureAboveOutside = 3.0f; // Room warmer than outside by this much
    private static final float highHumidity = 70f; // Uncomfortable above this
    private static final float humidityDelta = 10f; // Rise over the recording window
    private static final float lightsOnLux = 80f; // Lux above this means lights likely on
    private static final float lightAboveOutside = 50f; // Artificial lights supplementing bright natural light
    private static final float lightsExpectedNight = 10f; // Outside this dark means night

    public static class Analysis {
        public final boolean likelyOccupied;
        public final boolean heatingLikelyOn;
        public final boolean humidityLikelyHigh;
        public final boolean lightsLikelyOn;

        public Analysis(boolean likelyOccupied, boolean heatingLikelyOn, boolean humidityLikelyHigh, boolean lightsLikelyOn) {
            this.likelyOccupied = likelyOccupied;
            this.heatingLikelyOn = heatingLikelyOn;
            this.humidityLikelyHigh = humidityLikelyHigh;
            this.lightsLikelyOn = lightsLikelyOn;
        }
    }

    public Analysis analyse(SensorNode sensorNode, SensorNodeHistory sensorNodeHistory, float outsideTemperature, float outsideLux) {
        // Occupancy
        boolean occupied = sensorNodeHistory.motionFraction() >= occupiedMotionFraction;
        // Heating
        float gap = sensorNode.getTemperature() - outsideTemperature;
        float temperatureTrend = sensorNodeHistory.temperatureTrend();
        boolean heatingOn = gap > temperatureAboveOutside && temperatureTrend > temperatureDelta;
        // Humidity
        boolean humidityHigh = sensorNodeHistory.averageHumidity() > highHumidity || sensorNodeHistory.humidityTrend() > humidityDelta;
        // Lights
        float roomLux = sensorNodeHistory.averageLux();
        boolean isDarkOutside = outsideLux < lightsExpectedNight;
        boolean lightsOn = (isDarkOutside && roomLux > lightsOnLux) || (roomLux - outsideLux) > lightAboveOutside;
        return new Analysis(occupied, heatingOn, humidityHigh, lightsOn);
    }

    public boolean isNightTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= 18 || hour < 7;
    }

    public boolean isMorning() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= 7 && hour < 9;
    }

    public boolean isDay() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= 9 && hour < 17;
    }
}

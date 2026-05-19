package io.github.borngle.homehelper;

import java.util.ArrayDeque;
import java.util.Deque;

public class SensorNodeHistory {
    public static final int windowMinutes = 10;
    private static final int maximumEntries = (windowMinutes * 60) / 2; // Every 2 seconds

    public static class Recording {
        public final long timeStamp; // Milliseconds
        public final float temperature;
        public final float humidity;
        public final float lux;
        public final boolean motion;

        public Recording(long timeStamp, float temperature, float humidity, float lux, boolean motion) {
            this.timeStamp = timeStamp;
            this.temperature = temperature;
            this.humidity = humidity;
            this.lux = lux;
            this.motion = motion;
        }
    }

    private final Deque<Recording> recordings = new ArrayDeque<>();

    public void record(float temperature, float humidity, float lux, boolean motion) {
        Recording recording = new Recording(System.currentTimeMillis(), temperature, humidity, lux, motion);
        recordings.addLast(recording);
        while(recordings.size() > maximumEntries) {
            recordings.removeFirst();
        }
    }

    public Deque<Recording> getRecordings() {
        return recordings;
    }

    public float motionFraction() {
        // Fraction of recent recordings where motion was true
        if(recordings.isEmpty()) {
            return 0;
        }
        int motionCount = 0;
        for(Recording recording : recordings) {
            if(recording.motion) {
                motionCount += 1;
            }
        }
        return (float) motionCount / recordings.size();
    }

    public float averageHumidity() {
        // Average humidity over the window
        if(recordings.isEmpty()) {
            return 0;
        }
        float sum = 0;
        for(Recording recording : recordings) {
            sum += recording.humidity;
        }
        return sum / recordings.size();
    }

    public float humidityTrend() {
        // Latest subtract oldest reading (positive means rising)
        if(recordings.size() < 2) {
            return 0;
        }
        return recordings.peekLast().humidity - recordings.peekFirst().humidity;
    }

    public float averageLux() {
        // Average lux over the window
        if(recordings.isEmpty()) {
            return 0;
        }
        float sum = 0;
        for(Recording recording : recordings) {
            sum += recording.lux;
        }
        return sum / recordings.size();
    }

    public float luxTrend() {
        // Latest subtract oldest reading (positive means rising)
        if(recordings.size() < 2) {
            return 0;
        }
        Recording latest = recordings.peekLast();
        Recording older = null;
        int index = 0;
        int targetIndex = recordings.size() - 2; // 4 seconds back (2 samples at 2 second intervals)
        for(Recording recording : recordings) {
            if(index == targetIndex) {
                older = recording;
                break;
            }
            index++;
        }
        if(older == null) {
            return 0;
        }
        return latest.lux - older.lux;
    }

    public float averageTemperature() {
        // Average temperature over the window
        if(recordings.isEmpty()) {
            return 0;
        }
        float sum = 0;
        for(Recording recording : recordings) {
            sum += recording.temperature;
        }
        return sum / recordings.size();
    }

    public float temperatureTrend() {
        // Latest subtract oldest reading (positive means rising)
        if(recordings.size() < 2) {
            return 0;
        }
        return recordings.peekLast().temperature - recordings.peekFirst().temperature;
    }
}

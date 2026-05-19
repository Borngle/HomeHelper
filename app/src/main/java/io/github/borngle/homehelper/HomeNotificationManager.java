package io.github.borngle.homehelper;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

public class HomeNotificationManager {
    // Base notification IDs
    private static final int baseHeatingID = 1000;
    private static final int baseHumidityID = 2000;
    private static final int baseLightsID = 3000;

    private final Context context;
    private final NotificationManagerCompat notificationManagerCompat;
    private final RoomAnalyser roomAnalyser = new RoomAnalyser();

    // (sensorNode index + notification ID) and its cooldown
    private final Map<Integer, Long> cooldowns = new HashMap<>();
    private float heatingHoursToday = 0;
    private long lastHeatingIncrement = 0;
    private long lastResetDay = -1;

    private final SharedPreferences sharedPreferences;

    public HomeNotificationManager(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        heatingHoursToday = sharedPreferences.getFloat("heating_hours_today", 0);
        lastHeatingIncrement = sharedPreferences.getLong("last_heating_increment", 0);
        lastResetDay = sharedPreferences.getLong("last_reset_day", -1);
        this.notificationManagerCompat = NotificationManagerCompat.from(context);
    }

    public void evaluate(SensorNode sensorNode, SensorNodeHistory sensorNodeHistory, int position, float outsideTemperature, float outsideLux) {
        if(!sensorNode.isReachable() || sensorNodeHistory.getRecordings().size() < 10) {
            return;
        }
        boolean globalHeating = !("Off").equals(sharedPreferences.getString("notify_heating_global", "On"));
        boolean globalLights = !("Off").equals(sharedPreferences.getString("notify_lights_global", "On"));
        boolean globalHumidity = !("Off").equals(sharedPreferences.getString("notify_humidity_global", "On"));
        long cooldown = Long.parseLong(sharedPreferences.getString("notification_frequency", "1800000"));
        // Read heating limit (0 means disabled)
        float heatingLimitHours = 0;
        String raw = sharedPreferences.getString("daily_heating_limit", "0");
        float parsed = Float.parseFloat(raw);
        if(parsed > 0) {
            heatingLimitHours = parsed;
        }
        long today = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());
        if(today != lastResetDay) {
            heatingHoursToday = 0;
            lastResetDay = today;
            sharedPreferences.edit()
                    .putFloat("heating_hours_today", heatingHoursToday)
                    .putLong("last_heating_increment", lastHeatingIncrement)
                    .putLong("last_reset_day", lastResetDay)
                    .apply();
        }
        RoomAnalyser.Analysis analysis = roomAnalyser.analyse(sensorNode, sensorNodeHistory, outsideTemperature, outsideLux);
        sensorNode.setMotion(analysis.likelyOccupied);
        if(analysis.heatingLikelyOn) {
            sensorNode.setHeatingOn(true);
        }
        else if(sensorNodeHistory.temperatureTrend() < -0.3f) {
            sensorNode.setHeatingOn(false);
        }
        if(sensorNode.isHeatingOn()) {
            long now = System.currentTimeMillis();
            // Only increment once every 2 seconds globally
            if(now - lastHeatingIncrement >= 2000) {
                heatingHoursToday += ((float) 2 / 3600);
                lastHeatingIncrement = now;
                sharedPreferences.edit()
                        .putFloat("heating_hours_today", heatingHoursToday)
                        .putLong("last_heating_increment", lastHeatingIncrement)
                        .putLong("last_reset_day", lastResetDay)
                        .apply();
            }
        }
        // Can send these notifications
        boolean heating = sensorNode.getNotifyHeating() && globalHeating;
        boolean humidity = sensorNode.getNotifyHumidity() && globalHumidity;
        boolean lights = sensorNode.getNotifyLights() && globalLights;
        String room = sensorNode.getRoom();
        // Heating on in unoccupied room
        if(sensorNode.isHeatingOn() && !analysis.likelyOccupied && heating) {
            float difference = sensorNode.getTemperature() - sensorNode.getIdealTemperature();
            String temperatureInformation;
            if(difference == 0) {
                temperatureInformation = String.format("Room is at ideal temperature of %.1f°C", difference);
            }
            else if(difference > 0) {
                temperatureInformation = String.format("Room is %.1f°C above ideal", difference);
            }
            else {
                temperatureInformation = String.format("Room is %.1f°C below ideal", Math.abs(difference));
            }
            notify(
                    baseHeatingID + position, cooldown,
                    "Heating on in " + room,
                    "Heating seems to be running in an empty room; consider turning it off " + "(" +
                            temperatureInformation + ")"
            );
        }
        // Heating on and outside is moderate
        else if(sensorNode.isHeatingOn() && analysis.outsideWarm && heating) {
            float difference = sensorNode.getTemperature() - sensorNode.getIdealTemperature();
            String body = difference >= 0
                    ? String.format("Room is at %.1f°C, already at or above ideal", sensorNode.getTemperature())
                    : "Outside temperature is moderate and heating is on; turn heating off?";
            notify(
                    baseHeatingID + position + 100, cooldown,
                    "Heating on in " + room,
                    body
            );
        }
        // Heating limit exceeded
        if(heatingLimitHours > 0 && heatingHoursToday >= heatingLimitHours && heating && sensorNode.isHeatingOn()) {
            notify(
                    baseHeatingID + position + 200, cooldown,
                    "Heating limit exceeded",
                    String.format("Heating has run for %.1fh of %.1fh today", heatingHoursToday, heatingLimitHours)
            );
        }
        if(analysis.roomWarm && analysis.outsideWarm && heating) {
            notify(
                    baseHeatingID + position + 300, cooldown,
                    room + " is getting warm",
                    "Consider opening a window or turning on a fan"
            );
        }
        if(analysis.roomCold && heating) {
            notify(
                    baseHeatingID + position + 400, cooldown,
                    room + " is getting cold",
                    "Consider closing a window or turning the heating on"
            );
        }
        // High humidity, room unventilated
        if(analysis.humidityHigh && humidity) {
            notify(
                    baseHumidityID + position, cooldown,
                    "High humidity in " + room,
                    "Consider opening a window or turning on a dehumidifier to prevent mould"
            );
        }
        // Low humidity, air is dry
        if(analysis.humidityLow && humidity) {
            notify(baseHumidityID + position + 100, cooldown,
                    "Low humidity in " + room,
                    "The air is very dry; consider turning on a humidifier"
            );
        }
        // Lights left on in unoccupied room
        if(analysis.lightsLikelyOn && !analysis.likelyOccupied && lights) {
            notify(
                    baseLightsID + position, cooldown,
                    "Lights left on in " + room,
                    "Lights appear to be on in an empty room; consider turning them off"
            );
        }
        // Lights are on but it is bright outside
        if(analysis.lightsUnnecessary && lights) {
            notify(
                    baseLightsID + position + 100, cooldown,
                    "Lights are on in " + room,
                    "It is quite bright outside; consider turning them off"
            );
        }
        // Dark in an occupied room during the day
        if(analysis.roomDarkDuringDay && lights) {
            notify(
                    baseLightsID + position + 200, cooldown,
                    "It's dark in " + room,
                    "It could be good to let some light in; consider opening the blinds"
            );
        }
        // Obstructed
        if(analysis.obstructed && lights) {
            notify(baseLightsID + position + 300, cooldown,
                    room + " node obstructed",
                    "Cannot read light level until unobstructed"
            );
        }
    }

    private void notify(int notificationID, long cooldownTime, String title, String text) {
        long now = System.currentTimeMillis();
        Long lastFired = cooldowns.get(notificationID);
        if(lastFired != null && (now - lastFired) < cooldownTime) {
            return; // Still in cooldown
        }
        // Intent when user taps notification (resumes naturally)
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Lets system fire an intent later on behalf of app
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationID, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        // Constructs the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ROOM_CHANNEL_ID")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent).setAutoCancel(true);
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManagerCompat.notify(notificationID, builder.build());
        }
        cooldowns.put(notificationID, now);
    }
}

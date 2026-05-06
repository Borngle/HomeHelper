package io.github.borngle.homehelper;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.Map;

public class HomeNotificationManager {
    // Base notification IDs
    private static final int baseHeatingID = 1000;
    private static final int baseHumidityID = 2000;
    private static final int baseLightsID = 3000;

    // Notification cooldowns in milliseconds
    private static final long cooldownHeating = 30 * 60 * 1000; // 30 minutes
    private static final long cooldownHumidity = 10 * 60 * 1000; // 10 minutes
    private static final long cooldownLights = 10 * 60 * 1000; // 10 minutes

    private final Context context;
    private final NotificationManagerCompat notificationManagerCompat;
    private final RoomAnalyser roomAnalyser = new RoomAnalyser();

    // (sensorNode index + notification ID) and its cooldown
    private final Map<Integer, Long> cooldowns = new HashMap<>();

    public HomeNotificationManager(Context context) {
        this.context = context;
        this.notificationManagerCompat = NotificationManagerCompat.from(context);
    }

    public void evaluate(SensorNode sensorNode, SensorNodeHistory sensorNodeHistory, int position, float outsideTemp, float outsideLux) {
        // TODO: configurable thresholds and room preferences (plant mode, pet mode)
        if(!sensorNode.isReachable()) {
            return;
        }
        RoomAnalyser.Analysis analysis = roomAnalyser.analyse(sensorNode, sensorNodeHistory, outsideTemp, outsideLux);
        String room = sensorNode.getRoom();
        if(sensorNodeHistory.getRecordings().size() < 10) { // Minimum number of recordings
            return;
        }
        // Heating on in unoccupied room
        if(analysis.heatingLikelyOn && !analysis.likelyOccupied) {
            notify(
                    baseHeatingID + position, cooldownHeating,
                    "Heating on in " + room,
                    "Heating seems to be running in an empty room; consider turning it off"
            );
        }
        else if(analysis.heatingUnnecessary) {
            notify(
                    baseHeatingID + position + 100, cooldownHeating,
                    "Heating on in " + room,
                    "Outside temperature is moderate and heating is on; turn heating off?"
            );
        }
        // TODO: Heating on past set limit
        // High humidity, room unventilated
        if(analysis.humidityHigh) {
            notify(
                    baseHumidityID + position, cooldownHumidity,
                    "High humidity in " + room,
                    "Consider opening a window or turning on a dehumidifier to prevent mould"
            );
        }
        // Low humidity, air is dry
        if(analysis.humidityLow) {
            notify(baseHumidityID + position + 100, cooldownHumidity,
                    "Low humidity in " + room,
                    "The air is very dry; consider turning on a humidifier"
            );
        }
        // Lights left on in unoccupied room
        if(analysis.lightsLikelyOn && !analysis.likelyOccupied) {
            notify(
                    baseLightsID + position, cooldownLights,
                    "Lights left on in" + room,
                    "Lights appear to be on in an empty room; consider turning them off"
            );
        }
        // Lights are on but it is bright outside
        if(analysis.lightsUnnecessary) {
            notify(
                    baseLightsID + position + 100, cooldownLights,
                    "Lights are on in " + room,
                    "It is quite bright outside; consider turning them off"
            );
        }
        // Dark in an occupied room during the day
        if(analysis.roomDarkDuringDay) {
            notify(
                    baseLightsID + position + 200, cooldownLights,
                    "It's dark in " + room,
                    "It could be good to let some light in; consider opening the blinds"
            );
        }
        // Obstructed
        if(analysis.obstructed) {
            notify(baseLightsID + position + 300, cooldownLights,
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

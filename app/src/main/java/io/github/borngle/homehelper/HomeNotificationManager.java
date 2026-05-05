package io.github.borngle.homehelper;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.Map;

public class HomeNotificationManager {
    // Base notification IDs
    private static final int baseHeatingID = 100;
    private static final int baseHumidityID = 200;
    private static final int baseLightsID = 300;

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
        if(!sensorNode.isReachable()) {
            return;
        }
        RoomAnalyser.Analysis analysis = roomAnalyser.analyse(sensorNode, sensorNodeHistory, outsideTemp, outsideLux);
        String room = sensorNode.getRoom();
        if(sensorNodeHistory.getRecordings().size() < 10) { // Minimum number of recordings
            return;
        }
        // Lights left on in unoccupied room
        if(analysis.lightsLikelyOn && !analysis.likelyOccupied) {
            notify(
                    baseLightsID + position, cooldownLights,
                    "Lights left on in" + room,
                    "Lights appear to be on in an empty room"
            );
        }
        // TODO: Lights on and there is a lot of natural light already

        // Heating on in unoccupied room
        if(analysis.heatingLikelyOn && !analysis.likelyOccupied) {
            notify(
                    baseHeatingID + position, cooldownHeating,
                    "Heating on in " + room,
                    "Heating seems to be running in an empty room"
            );
        }
        // TODO: Heating on past set limit

        // High humidity, room unventilated
        if(analysis.humidityLikelyHigh) {
            notify(
                    baseHumidityID + position, cooldownHumidity,
                    "High humidity in " + room,
                    "Humidity is around " + sensorNode.getHumidity() + "%"
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

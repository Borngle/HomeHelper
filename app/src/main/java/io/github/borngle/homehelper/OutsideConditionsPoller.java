package io.github.borngle.homehelper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OutsideConditionsPoller {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS) // Fails if no connection
            .readTimeout(3, TimeUnit.SECONDS) // Fails if connected but no response
            .build();
    private final Context context;
    private final OutsideConditions outsideConditions;

    public OutsideConditionsPoller(Context context, OutsideConditions outsideConditions) {
        this.context = context;
        this.outsideConditions = outsideConditions;
    }

    public void poll() {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
            if (location == null) {
                return;
            }
            String url = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + location.getLatitude()
                    + "&longitude=" + location.getLongitude()
                    + "&current=temperature_2m,shortwave_radiation"
                    + "&forecast_days=1";
            Request get = new Request.Builder()
                    .url(url)
                    .build();
            client.newCall(get).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        ResponseBody body = response.body();
                        if(!response.isSuccessful()) {
                            return;
                        }
                        JSONObject json = new JSONObject(body.string()).getJSONObject("current");
                        float temperature = (float) json.getDouble("temperature_2m");
                        float lux = (float) json.getDouble("shortwave_radiation") * 120f;
                        outsideConditions.setTemperature(temperature);
                        outsideConditions.setLux(lux);
                    }
                    catch(Exception e) {

                    }
                }
            });
        });
    }
}
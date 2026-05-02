package io.github.borngle.homehelper;

import android.os.Handler;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class SensorNodePoller {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS) // Fails if no connection
            .readTimeout(3, TimeUnit.SECONDS) // Fails if connected but no response
            .build();
    private final ArrayList<SensorNode> sensorNodes;
    private final SensorNodeAdapter adapter;
    private final Handler mainHandler;

    SensorNodePoller(ArrayList<SensorNode> sensorNodes, SensorNodeAdapter adapter, Handler mainHandler) {
        this.sensorNodes = sensorNodes;
        this.adapter = adapter;
        this.mainHandler = mainHandler;
    }

    public void pollAll() {
        for(int i = 0; i < sensorNodes.size(); i++) {
            pollNode(i);
        }
    }

    public void pollNode(int position) {
        SensorNode sensorNode = sensorNodes.get(position);
        String url = sensorNode.getUrl();
        if(url == null || url.isEmpty()) {
            return;
        }
        Request get = new Request.Builder()
                .url(sensorNode.getUrl())
                .build();
        client.newCall(get).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                sensorNode.setReachable(false);
                mainHandler.post(() -> adapter.updateSensorNode(position));
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    ResponseBody responseBody = response.body();
                    if(!response.isSuccessful() || responseBody == null) {
                        sensorNode.setReachable(false);
                        return;
                    }
                    JSONObject json = new JSONObject(responseBody.string());
                    sensorNode.setTemperature((float) json.getDouble("temperature"));
                    sensorNode.setHumidity((float) json.getDouble("humidity"));
                    sensorNode.setLux((float) json.getDouble("lux"));
                    sensorNode.setMotion(json.getBoolean("motion"));
                    sensorNode.setReachable(true);
                }
                catch(Exception e) {
                    sensorNode.setReachable(false);
                }
                finally {
                    mainHandler.post(() -> adapter.updateSensorNode(position));
                }
            }
        });
    }
}

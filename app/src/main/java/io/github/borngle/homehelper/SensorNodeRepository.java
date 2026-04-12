package io.github.borngle.homehelper;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SensorNodeRepository {
    private final SharedPreferences sharedPreferences;

    public SensorNodeRepository(Context context) {
        sharedPreferences = context.getSharedPreferences("sensor_nodes", Context.MODE_PRIVATE);
    }

    public ArrayList<SensorNode> loadSensorNodes() {
        ArrayList<SensorNode> sensorNodes = new ArrayList<>();
        String json = sharedPreferences.getString("nodes", null);
        if(json == null) {
            return sensorNodes;
        }
        try {
            JSONArray jsonArray = new JSONArray(json);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SensorNode sensorNode = new SensorNode(
                        jsonObject.getString("room"),
                        jsonObject.getString("url")
                );
                sensorNodes.add(sensorNode);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return sensorNodes;
    }

    public void saveSensorNodes(ArrayList<SensorNode> sensorNodes) {
        try {
            JSONArray jsonArray = new JSONArray();
            for(int i = 0; i < sensorNodes.size(); i++) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("room", sensorNodes.get(i).getRoom());
                jsonObject.put("url", sensorNodes.get(i).getUrl());
                jsonArray.put(jsonObject);
            }
            sharedPreferences.edit().putString("nodes", jsonArray.toString());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

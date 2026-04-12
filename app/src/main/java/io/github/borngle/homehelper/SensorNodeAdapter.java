package io.github.borngle.homehelper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// Adapter class
public class SensorNodeAdapter extends RecyclerView.Adapter<SensorNodeAdapter.SensorNodeViewHolder> {
    private ArrayList<SensorNode> sensorNodes;

    public SensorNodeAdapter(ArrayList<SensorNode> sensorNodes) {
        this.sensorNodes = sensorNodes;
    }

    // Card holding values for a SensorNode
    class SensorNodeViewHolder extends RecyclerView.ViewHolder {
        TextView roomText, temperatureText, humidityText, motionText;

        public SensorNodeViewHolder(View sensorNodeView) {
            super(sensorNodeView);
            roomText = sensorNodeView.findViewById(R.id.roomText);
            temperatureText = sensorNodeView.findViewById(R.id.temperatureText);
            humidityText = sensorNodeView.findViewById(R.id.humidityText);
            motionText = sensorNodeView.findViewById(R.id.motionText);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SensorNodeViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View sensorNodeView = LayoutInflater.from(viewGroup.getContext()) // Builds view from XML
                .inflate(R.layout.sensor_node, viewGroup, false);
        int screenHeight = viewGroup.getResources().getDisplayMetrics().heightPixels;
        sensorNodeView.getLayoutParams().height = screenHeight / 3;
        return new SensorNodeViewHolder(sensorNodeView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(SensorNodeViewHolder sensorNodeViewHolder, int position) {
        SensorNode sensorNode = sensorNodes.get(position);
        sensorNodeViewHolder.roomText.setText(sensorNode.getRoom());
        if(sensorNode.isReachable()) {
            sensorNodeViewHolder.temperatureText.setText("Temperature: " + sensorNode.getTemperature() + "°C");
            sensorNodeViewHolder.humidityText.setText("Humidity: " + sensorNode.getHumidity() + "%");
            sensorNodeViewHolder.motionText.setText(sensorNode.isMotion() ? "Room occupied" : "Empty room");
        }
        else {
            sensorNodeViewHolder.temperatureText.setText("--");
            sensorNodeViewHolder.humidityText.setText("--");
            sensorNodeViewHolder.motionText.setText("Unreachable");
        }
    }

    // Return the number of nodes (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return sensorNodes.size();
    }

    // Called after a poll updates the sensor values
    public void updateSensorNode(int position) {
        notifyItemChanged(position); // This tells RecyclerView to call onBindViewHolder
    }
}

package io.github.borngle.homehelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;

public class EditSensorNodeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_sensor_node);
        MaterialToolbar materialToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(materialToolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        int position = getIntent().getIntExtra("position", -1);
        String room = getIntent().getStringExtra("room");
        SensorNodeRepository sensorNodeRepository = new SensorNodeRepository(this);
        if(position == -1 || room == null) {
            finish();
            return;
        }
        EditText roomInput = findViewById(R.id.roomInput);
        roomInput.setText(room); // Existing name
        MaterialSwitch switchHeating = findViewById(R.id.switchHeating);
        MaterialSwitch switchHumidity = findViewById(R.id.switchHumidity);
        MaterialSwitch switchLights = findViewById(R.id.switchLights);
        switchHeating.setChecked(getIntent().getBooleanExtra("notifyHeating", true));
        switchHumidity.setChecked(getIntent().getBooleanExtra("notifyHumidity", true));
        switchLights.setChecked(getIntent().getBooleanExtra("notifyLights", true));
        findViewById(R.id.save).setOnClickListener(v -> {
            String newRoom = roomInput.getText().toString();
            if(newRoom.isEmpty()) {
                roomInput.setError("Room name cannot be blank");
                return;
            }
            ArrayList<SensorNode> sensorNodes = sensorNodeRepository.loadSensorNodes();
            if(position < sensorNodes.size()) {
                sensorNodes.get(position).setRoom(newRoom);
                sensorNodeRepository.saveSensorNodes(sensorNodes);
            }
            Intent intent = new Intent();
            intent.putExtra("position", position);
            intent.putExtra("room", newRoom);
            intent.putExtra("notifyHeating", sensorNodes.get(position).getNotifyHeating());
            intent.putExtra("notifyHumidity", sensorNodes.get(position).getNotifyHumidity());
            intent.putExtra("notifyLights", sensorNodes.get(position).getNotifyLights());
            setResult(RESULT_OK, intent);
            finish();
        });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
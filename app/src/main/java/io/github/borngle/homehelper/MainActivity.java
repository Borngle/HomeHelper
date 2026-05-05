package io.github.borngle.homehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SensorNodeAdapter adapter;
    private ArrayList<SensorNode> sensorNodes;
    private Handler sensorNodeHandler;
    private Runnable sensorNodeRunnable;
    private Handler weatherHandler;
    private Runnable weatherRunnable;
    private final OutsideConditions outsideConditions = new OutsideConditions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        createNotificationChannel(this);
        // Test node
        SensorNodeRepository sensorNodeRepository = new SensorNodeRepository(this);
        sensorNodes = sensorNodeRepository.loadSensorNodes();
        if(sensorNodes.isEmpty()) { // Just for testing
            sensorNodes.add(new SensorNode("Living Room", ""));
            sensorNodeRepository.saveSensorNodes(sensorNodes);
        }
        adapter = new SensorNodeAdapter(sensorNodes);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        ActivityResultLauncher<Intent> editSensorNodeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == RESULT_OK && result.getData() != null) {
                int position = result.getData().getIntExtra("position", -1);
                String newRoom = result.getData().getStringExtra("room");
                sensorNodes.get(position).setRoom(newRoom);
                adapter.notifyItemChanged(position);
            }
        });
        adapter.setOnSensorNodeClick(position -> {
            Intent intent = new Intent(this, EditSensorNodeActivity.class);
            intent.putExtra("position", position);
            intent.putExtra("room", sensorNodes.get(position).getRoom());
            editSensorNodeLauncher.launch(intent);
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        weatherHandler = new Handler(Looper.getMainLooper());
        OutsideConditionsPoller outsideConditionsPoller = new OutsideConditionsPoller(this, outsideConditions);
        weatherRunnable = new Runnable() {
            @Override
            public void run() {
                outsideConditionsPoller.poll();
                weatherHandler.postDelayed(this, 5 * 60 * 1000);
            }
        };
        weatherHandler.post(weatherRunnable);
        sensorNodeHandler = new Handler(Looper.getMainLooper());
        SensorNodePoller sensorNodePoller = new SensorNodePoller(this, sensorNodes, adapter, sensorNodeHandler, outsideConditions);
        sensorNodeRunnable = new Runnable() {
            @Override
            public void run() {
                sensorNodePoller.pollAll();
                sensorNodeHandler.postDelayed(this, 2000);
            }
        };
        sensorNodeHandler.post(sensorNodeRunnable);
    }

    private void createNotificationChannel(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Room Alerts";
            String descriptionText = "Notifications for room alerts";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("ROOM_CHANNEL_ID", name, importance);
            channel.setDescription(descriptionText);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorNodeHandler.removeCallbacks(sensorNodeRunnable);
        weatherHandler.removeCallbacks(weatherRunnable);
    }
}
package io.github.borngle.homehelper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SensorNodeAdapter adapter;
    private ArrayList<SensorNode> sensorNodes;
    private Handler handler;
    private Runnable pollRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Test node
        sensorNodes = new ArrayList<>();
        sensorNodes.add(new SensorNode("Living Room", ""));
        adapter = new SensorNodeAdapter(sensorNodes);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        handler = new Handler(Looper.getMainLooper());
        SensorNodePoller poller = new SensorNodePoller(sensorNodes, adapter, handler);
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                poller.pollAll();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(pollRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollRunnable);
    }
}
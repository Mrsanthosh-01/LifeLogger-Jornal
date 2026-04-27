package com.example.lifelogger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MemoryListActivity extends AppCompatActivity {

    RecyclerView recycler;
    List<Memory> list = new ArrayList<>();
    ProgressBar loader;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_memory_list);

        recycler = findViewById(R.id.recycler);
        loader = new ProgressBar(this); // Optional: add to XML for better look
        
        recycler.setLayoutManager(new LinearLayoutManager(this));

        loadDataInBackground();
    }

    private void loadDataInBackground() {
        new Thread(() -> {
            SharedPreferences p = getSharedPreferences("LifeLogger", MODE_PRIVATE);
            String json = p.getString("memories_json", "[]");

            List<Memory> tempList = new ArrayList<>();
            try {
                JSONArray arr = new JSONArray(json);
                // Load in reverse to show latest memories first
                for (int i = arr.length() - 1; i >= 0; i--) {
                    JSONObject o = arr.getJSONObject(i);
                    tempList.add(new Memory(
                            o.getString("image"),
                            o.getString("location"),
                            o.getString("time"),
                            o.getString("note"),
                            o.optString("audio", ""),
                            o.optString("mood", "Normal")
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                list.clear();
                list.addAll(tempList);
                recycler.setAdapter(new MemoryAdapter(list));
            });
        }).start();
    }
}

package com.example.lifelogger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MemoriesActivity extends AppCompatActivity {

    TextView memoriesDisplay;
    Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memories);

        memoriesDisplay = findViewById(R.id.memoriesDisplay);
        backButton = findViewById(R.id.backButton);

        SharedPreferences prefs = getSharedPreferences("LifeLogger", MODE_PRIVATE);
        String memories = prefs.getString("memories", "No memories yet.");

        memoriesDisplay.setText(memories);

        backButton.setOnClickListener(v -> finish());
    }
}

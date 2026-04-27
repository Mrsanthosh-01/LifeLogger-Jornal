package com.example.lifelogger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class LockActivity extends AppCompatActivity {

    private String currentPin = "";
    private String savedPin = "";
    private final List<View> dots = new ArrayList<>();
    private TextView title, subtitle;
    private boolean isSettingFirstTime = false;
    private String firstEnteredPin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = getSharedPreferences("Security", MODE_PRIVATE);
        boolean isLockEnabled = prefs.getBoolean("lock_enabled", false);
        savedPin = prefs.getString("pin", "");

        // If lock is not enabled and PIN exists, or if no PIN exists, we might want to skip or go to Main
        // But for this task, let's assume if it's the launcher, we check if PIN is set.
        if (savedPin.isEmpty()) {
            // First time or no PIN, go to Main if we don't want to force lock
            // Or let user set it. Let's let user set it for now.
            isSettingFirstTime = true;
        } else if (!isLockEnabled) {
            // Lock exists but disabled, go to Main
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_lock);

        title = findViewById(R.id.lockTitle);
        subtitle = findViewById(R.id.lockSubtitle);

        dots.add(findViewById(R.id.dot1));
        dots.add(findViewById(R.id.dot2));
        dots.add(findViewById(R.id.dot3));
        dots.add(findViewById(R.id.dot4));

        if (isSettingFirstTime) {
            title.setText("SET UP PIN");
            subtitle.setText("Choose a 4-digit PIN for security");
        } else {
            title.setText("SECURE ACCESS");
            subtitle.setText("Enter your 4-digit PIN");
        }

        setupButtons();
    }

    private void setupButtons() {
        View.OnClickListener listener = v -> {
            if (currentPin.length() < 4) {
                String digit = (String) v.getTag();
                currentPin += digit;
                updateDots();
                if (currentPin.length() == 4) {
                    processPin();
                }
            }
        };

        findButtons(getWindow().getDecorView(), listener);

        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (!currentPin.isEmpty()) {
                currentPin = currentPin.substring(0, currentPin.length() - 1);
                updateDots();
            }
        });
    }

    private void findButtons(View v, View.OnClickListener listener) {
        if (v instanceof Button) {
            Button b = (Button) v;
            if (b.getTag() != null) {
                b.setOnClickListener(listener);
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                findButtons(group.getChildAt(i), listener);
            }
        }
    }

    private void updateDots() {
        for (int i = 0; i < dots.size(); i++) {
            dots.get(i).setAlpha(i < currentPin.length() ? 1.0f : 0.3f);
        }
    }

    private void processPin() {
        if (isSettingFirstTime) {
            if (firstEnteredPin.isEmpty()) {
                firstEnteredPin = currentPin;
                currentPin = "";
                updateDots();
                title.setText("CONFIRM PIN");
                subtitle.setText("Enter the PIN again to confirm");
            } else {
                if (firstEnteredPin.equals(currentPin)) {
                    SharedPreferences.Editor editor = getSharedPreferences("Security", MODE_PRIVATE).edit();
                    editor.putString("pin", currentPin);
                    editor.putBoolean("lock_enabled", true);
                    editor.apply();
                    Toast.makeText(this, "PIN Set Successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "PINs do not match. Try again.", Toast.LENGTH_SHORT).show();
                    currentPin = "";
                    firstEnteredPin = "";
                    updateDots();
                    title.setText("SET UP PIN");
                }
            }
        } else {
            if (savedPin.equals(currentPin)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                currentPin = "";
                updateDots();
            }
        }
    }
}

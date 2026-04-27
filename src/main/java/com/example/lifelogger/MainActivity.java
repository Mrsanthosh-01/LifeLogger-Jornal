package com.example.lifelogger;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private EditText noteInput;
    private TextView txtLocation, statusText, profileName, syncStatus;
    private ImageView imagePreview, profileImg;
    private ProgressBar saveProgress;
    private ChipGroup moodGroup;
    private ImageButton btnThemeToggle;
    private Button btnGoogleLogin, btnSave, btnView, btnPlayPreview;
    private View cardLocation, cardPhoto, cardVoice, cardTyping;

    private String currentPlace = "Unknown";
    private String imagePath = "";
    private String audioPath = "";

    private FusedLocationProviderClient fusedLocationClient;
    private MediaRecorder recorder;
    private android.media.MediaPlayer mediaPlayer;
    private boolean isRecording = false;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        SharedPreferences uiPrefs = getSharedPreferences("UI", MODE_PRIVATE);
        boolean isDarkMode = uiPrefs.getBoolean("dark_mode", true);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupGoogleSignIn();
        setupListeners();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initViews() {
        noteInput = findViewById(R.id.noteInput);
        txtLocation = findViewById(R.id.txtLocation);
        statusText = findViewById(R.id.statusText);
        profileName = findViewById(R.id.profileName);
        syncStatus = findViewById(R.id.syncStatus);
        imagePreview = findViewById(R.id.imagePreview);
        profileImg = findViewById(R.id.profileImg);
        saveProgress = findViewById(R.id.saveProgress);
        moodGroup = findViewById(R.id.moodGroup);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnSave = findViewById(R.id.btnSave);
        btnView = findViewById(R.id.btnView);
        btnPlayPreview = findViewById(R.id.btnPlayPreview);

        cardLocation = findViewById(R.id.cardLocation);
        cardPhoto = findViewById(R.id.cardPhoto);
        cardVoice = findViewById(R.id.cardVoice);
        cardTyping = findViewById(R.id.cardTyping);
    }

    private void setupListeners() {
        cardLocation.setOnClickListener(v -> getLocation());
        cardPhoto.setOnClickListener(v -> openGallery());
        cardVoice.setOnClickListener(v -> toggleRecording());
        cardTyping.setOnClickListener(v -> startVoiceTyping());

        btnSave.setOnClickListener(v -> saveMemory());
        btnView.setOnClickListener(v -> startActivity(new Intent(this, MemoryListActivity.class)));
        btnPlayPreview.setOnClickListener(v -> playPreview());

        btnThemeToggle.setOnClickListener(v -> toggleTheme());
        btnGoogleLogin.setOnClickListener(v -> signIn());
    }

    private void toggleTheme() {
        SharedPreferences uiPrefs = getSharedPreferences("UI", MODE_PRIVATE);
        boolean currentMode = uiPrefs.getBoolean("dark_mode", true);
        uiPrefs.edit().putBoolean("dark_mode", !currentMode).apply();
        
        // Re-apply and recreate
        AppCompatDelegate.setDefaultNightMode(!currentMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            updateUI(account);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            updateUI(account);
            Toast.makeText(this, "Backup account linked!", Toast.LENGTH_SHORT).show();
        } catch (ApiException e) {
            statusText.setText("Google Sign-in failed ❌");
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        if (account != null) {
            profileName.setText(account.getDisplayName());
            syncStatus.setText("Synced: " + account.getEmail());
            btnGoogleLogin.setText("Sign Out");
            btnGoogleLogin.setOnClickListener(v -> signOut());
        } else {
            profileName.setText("Guest User");
            syncStatus.setText("Not synced with Google");
            btnGoogleLogin.setText("Backup");
            btnGoogleLogin.setOnClickListener(v -> signIn());
        }
    }

    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> updateUI(null));
    }

    // Existing features logic (Location, Photo, Voice) updated with new Views if needed
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        statusText.setText("Locating... 📍");
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                new Thread(() -> {
                    try {
                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            currentPlace = addresses.get(0).getLocality();
                            runOnUiThread(() -> {
                                txtLocation.setText("Location: " + currentPlace);
                                statusText.setText("Location found ✅");
                            });
                        }
                    } catch (IOException e) {
                        runOnUiThread(() -> statusText.setText("Geocoder error ❌"));
                    }
                }).start();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri imageUri = result.getData().getData();
            imagePreview.setImageURI(imageUri);
            imagePath = imageUri != null ? imageUri.toString() : "";
            statusText.setText("Photo added ✅");
        }
    });

    private void startVoiceTyping() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                noteInput.setText(results.get(0));
            }
        }
    });

    private void toggleRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 102);
            return;
        }
        if (!isRecording) startRecording(); else stopRecording();
    }

    private void startRecording() {
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "voice_" + System.currentTimeMillis() + ".3gp");
            audioPath = file.getAbsolutePath();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(audioPath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.prepare();
            recorder.start();
            isRecording = true;
            statusText.setText("Recording... 🎙️");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopRecording() {
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception e) {}
            recorder.release();
            recorder = null;
        }
        isRecording = false;
        statusText.setText("Voice note saved ✅");
        btnPlayPreview.setVisibility(View.VISIBLE);
    }

    private void playPreview() {
        if (audioPath == null || audioPath.isEmpty()) return;
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = new android.media.MediaPlayer();
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            statusText.setText("Playing preview... 🔊");
            mediaPlayer.setOnCompletionListener(mp -> {
                statusText.setText("Preview finished.");
                mp.release();
                mediaPlayer = null;
            });
        } catch (IOException e) { statusText.setText("Playback error ❌"); }
    }

    private void saveMemory() {
        statusText.setText("Saving... ⏳");
        saveProgress.setVisibility(View.VISIBLE);
        int checkedId = moodGroup.getCheckedChipId();
        String mood = "Normal";
        if (checkedId != View.NO_ID) {
            Chip chip = findViewById(checkedId);
            mood = chip.getText().toString();
        }
        final String selectedMood = mood;
        new Thread(() -> {
            try {
                String note = noteInput.getText().toString().trim();
                String time = new SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(new Date());
                SharedPreferences prefs = getSharedPreferences("LifeLogger", MODE_PRIVATE);
                String json = prefs.getString("memories_json", "[]");
                JSONArray arr = new JSONArray(json);
                JSONObject obj = new JSONObject();
                obj.put("location", currentPlace);
                obj.put("time", time);
                obj.put("note", note);
                obj.put("image", imagePath);
                obj.put("audio", audioPath);
                obj.put("mood", selectedMood);
                arr.put(obj);
                prefs.edit().putString("memories_json", arr.toString()).apply();
                Thread.sleep(500);
                runOnUiThread(() -> {
                    saveProgress.setVisibility(View.GONE);
                    statusText.setText("Saved at " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()) + " ✅");
                    noteInput.setText("");
                    moodGroup.clearCheck();
                    imagePreview.setImageResource(R.drawable.ic_placeholder);
                    imagePath = "";
                    audioPath = "";
                    btnPlayPreview.setVisibility(View.GONE);
                    currentPlace = "Unknown";
                    txtLocation.setText("Location: -");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saveProgress.setVisibility(View.GONE);
                    statusText.setText("Save failed ❌");
                });
            }
        }).start();
    }
}

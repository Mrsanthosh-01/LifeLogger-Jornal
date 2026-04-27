package com.example.lifelogger;

import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.ViewHolder> {

    List<Memory> list;
    private MediaPlayer mediaPlayer;

    public MemoryAdapter(List<Memory> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView location, time, note, mood;
        Button btnPlayAudio;
        View audioContainer;

        public ViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.memoryImage);
            location = v.findViewById(R.id.memoryPlace);
            time = v.findViewById(R.id.memoryTime);
            note = v.findViewById(R.id.memoryNote);
            mood = v.findViewById(R.id.memoryMood);
            btnPlayAudio = v.findViewById(R.id.btnPlayAudio);
            audioContainer = v.findViewById(R.id.audioContainer);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {
        Memory m = list.get(i);

        if (m.image != null && !m.image.isEmpty()) {
            h.img.setImageURI(Uri.parse(m.image));
        } else {
            h.img.setImageResource(R.drawable.ic_placeholder);
        }
        
        h.location.setText(m.location);
        h.time.setText(m.time);
        h.note.setText(m.note);
        if (h.mood != null) {
            h.mood.setText("Feeling: " + m.mood);
        }

        if (m.audio != null && !m.audio.isEmpty() && new File(m.audio).exists()) {
            h.audioContainer.setVisibility(View.VISIBLE);
            h.btnPlayAudio.setOnClickListener(v -> playAudio(m.audio, v.getContext(), h.btnPlayAudio));
        } else {
            h.audioContainer.setVisibility(View.GONE);
        }
    }

    private void playAudio(String path, android.content.Context context, Button playBtn) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            playBtn.setText("⏸ Playing...");
            Toast.makeText(context, "Playing audio...", Toast.LENGTH_SHORT).show();
            
            mediaPlayer.setOnCompletionListener(mp -> {
                playBtn.setText("▶ Play Voice Note");
                mp.release();
                mediaPlayer = null;
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}

package com.example.lifelogger;

public class Memory {
    public String image;
    public String location;
    public String time;
    public String note;
    public String audio;
    public String mood;

    public Memory(String image, String location, String time, String note, String audio, String mood) {
        this.image = image;
        this.location = location;
        this.time = time;
        this.note = note;
        this.audio = audio;
        this.mood = mood;
    }
}

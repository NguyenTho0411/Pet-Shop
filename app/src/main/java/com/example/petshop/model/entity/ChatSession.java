package com.example.petshop.model.entity;

public class ChatSession {
    private String id;
    private String title;
    private long   lastTimestamp;

    public ChatSession() {}

    public ChatSession(String id, String title) {
        this.id = id;
        this.title = title;
        this.lastTimestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }
}

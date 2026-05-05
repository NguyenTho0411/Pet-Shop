package com.example.petshop.model.entity;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT  = 1;

    private String text;
    private String imageUrl;
    private int    type;
    private long   timestamp;
    private String sessionId;

    public ChatMessage() {} // Required for Firestore

    public ChatMessage(String text, int type) {
        this(text, null, type);
    }

    public ChatMessage(String text, String imageUrl, int type) {
        this.text      = text;
        this.imageUrl  = imageUrl;
        this.type      = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}

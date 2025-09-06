package org.example.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;
    private String userId;
    private String sessionId;
    private String type; // "USER" or "AI"
    private String content;
    private LocalDateTime timestamp;
    private String metadata;

    // ✅ Default no-args constructor (REQUIRED by Spring Data/Jackson)
    public ChatMessage() {}

    // ✅ 2-parameter constructor for compatibility with ChatSession
    public ChatMessage(String type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // ✅ 3-parameter constructor for compatibility with ChatSession
    public ChatMessage(String userId, String type, String content) {
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // ✅ Full 4-parameter constructor
    public ChatMessage(String userId, String sessionId, String type, String content) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.type = type;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
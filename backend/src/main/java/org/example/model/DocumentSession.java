package org.example.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "document_sessions")
public class DocumentSession {
    @Id
    private String id;
    private String userId;
    private String sessionTitle;
    private String dayKey; // "2025-09-03"
    private String chatSessionId; // Link to ChatSession
    private List<String> documentIds = new ArrayList<>();
    private List<SearchQuery> searchHistory = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;

    // Constructors
    public DocumentSession() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.dayKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public DocumentSession(String userId, String sessionTitle) {
        this();
        this.userId = userId;
        this.sessionTitle = sessionTitle;
        generateId();
    }

    private void generateId() {
        if (this.userId != null) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            this.id = "doc_" + userId + "_" + dayKey + "_" + timestamp;
        }
    }

    // Helper methods
    public void addDocument(String documentId) {
        if (!this.documentIds.contains(documentId)) {
            this.documentIds.add(documentId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void addSearchQuery(SearchQuery query) {
        this.searchHistory.add(query);
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
    public String getDayKey() { return dayKey; }
    public void setDayKey(String dayKey) { this.dayKey = dayKey; }
    public String getChatSessionId() { return chatSessionId; }
    public void setChatSessionId(String chatSessionId) { this.chatSessionId = chatSessionId; }
    public List<String> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }
    public List<SearchQuery> getSearchHistory() { return searchHistory; }
    public void setSearchHistory(List<SearchQuery> searchHistory) { this.searchHistory = searchHistory; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}

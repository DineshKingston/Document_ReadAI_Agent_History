package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SearchQuery {
    private String id;
    private String query;
    private String queryType; // "KEYWORD", "AI_QUESTION", "SEMANTIC"
    private LocalDateTime timestamp;
    private int resultsCount;
    private double executionTime; // in milliseconds
    private String userId; // For user isolation
    private String sessionId; // Reference to parent session

    public SearchQuery() {
        this.timestamp = LocalDateTime.now();
        this.id = generateId();
    }

    public SearchQuery(String query, String queryType) {
        this();
        this.query = query;
        this.queryType = queryType;
    }

    public SearchQuery(String query, String queryType, int resultsCount, LocalDateTime timestamp) {
        this(query, queryType);
        this.resultsCount = resultsCount;
        this.timestamp = timestamp;
    }

    private String generateId() {
        return "search_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public int getResultsCount() { return resultsCount; }
    public void setResultsCount(int resultsCount) { this.resultsCount = resultsCount; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public double getExecutionTime() { return executionTime; }
    public void setExecutionTime(double executionTime) { this.executionTime = executionTime; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")) : "";
    }
}

package org.example.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "search_history")
public class SearchHistory {
    @Id
    private String id;
    private String userId;
    private String sessionId;
    private String query;
    private int resultsCount;
    private String queryType;
    private LocalDateTime timestamp;

    // Default constructor
    public SearchHistory() {}

    // Parameterized constructor
    public SearchHistory(String userId, String sessionId, String query, int resultsCount) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.query = query;
        this.resultsCount = resultsCount;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getResultsCount() { return resultsCount; }
    public void setResultsCount(int resultsCount) { this.resultsCount = resultsCount; }

    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
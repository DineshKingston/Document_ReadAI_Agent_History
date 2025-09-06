package org.example.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;
    private String userId;
    private String sessionTitle;
    private String sessionType; // "DOCUMENT_ANALYSIS", "AI_CHAT", "SEARCH", "UNIFIED_SESSION"
    private String dayKey; // "2025-09-03" for day-wise separation
    private String timeSlot; // "10:30" for time-based separation within day

    // Core session data
    private List<ChatMessage> messages = new ArrayList<>();
    private List<String> documentIds = new ArrayList<>();
    private List<DocumentInfo> documentDetails = new ArrayList<>();
    private List<AIResponse> aiResponses = new ArrayList<>();
    private List<SearchQuery> searchQueries = new ArrayList<>();

    // Session metadata
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessedAt;

    // Session status
    private boolean isActive;
    private String status; // "ACTIVE", "ARCHIVED", "DELETED"
    private String sessionSummary;

    // Session statistics
    private SessionStats stats;

    // Session restoration data
    private RestorationData restorationData;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    public ChatSession() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.isActive = true;
        this.status = "ACTIVE";
        this.dayKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.timeSlot = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        this.stats = new SessionStats();
        this.restorationData = new RestorationData();
        generateSessionId();
    }

    public ChatSession(String userId, String sessionTitle, String sessionType) {
        this();
        this.userId = userId;
        this.sessionTitle = sessionTitle;
        this.sessionType = sessionType;
        generateSessionId();
    }

    // ============================================
    // SESSION ID GENERATION
    // ============================================

    private void generateSessionId() {
        if (this.userId != null) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String randomSuffix = String.valueOf((int)(Math.random() * 1000));
            this.id = userId + "_" + dayKey + "_" + sessionType + "_" + timestamp + "_" + randomSuffix;
        }
    }

    // ============================================
    // ENHANCED DOCUMENT MANAGEMENT
    // ============================================

    public void addDocument(String documentId, String fileName, String fileType) {
        if (!this.documentIds.contains(documentId)) {
            this.documentIds.add(documentId);

            DocumentInfo docInfo = new DocumentInfo(documentId, fileName, fileType, LocalDateTime.now());
            this.documentDetails.add(docInfo);

            this.updatedAt = LocalDateTime.now();
            this.stats.incrementDocumentCount();
            this.restorationData.addDocumentForRestoration(documentId, fileName, fileType, null);

            // Update session title if it's a document session
            if ("DOCUMENT_ANALYSIS".equals(this.sessionType) && this.documentDetails.size() == 1) {
                this.sessionTitle = "Documents - " + fileName + " - " + this.dayKey + " " + this.timeSlot;
            }
        }
    }

    // ✅ ENHANCED: Add document with full content for proper restoration
    public void addDocumentWithContent(String documentId, String fileName, String fileType, String textContent, Long fileSize) {
        if (!this.documentIds.contains(documentId)) {
            this.documentIds.add(documentId);

            DocumentInfo docInfo = new DocumentInfo(documentId, fileName, fileType, LocalDateTime.now());
            docInfo.setFileSize(fileSize != null ? fileSize : (textContent != null ? (long) textContent.length() : 0L));
            docInfo.setTextContent(textContent); // ✅ Store actual text content
            docInfo.setContentLength(textContent != null ? textContent.length() : 0);

            this.documentDetails.add(docInfo);
            this.updatedAt = LocalDateTime.now();
            this.stats.incrementDocumentCount();
            this.restorationData.addDocumentForRestoration(documentId, fileName, fileType, textContent);

            System.out.println("✅ Stored document with content: " + fileName + " (" + (textContent != null ? textContent.length() : 0) + " chars)");

            // Update session title if it's a document session
            if ("DOCUMENT_ANALYSIS".equals(this.sessionType) && this.documentDetails.size() == 1) {
                this.sessionTitle = "Documents - " + fileName + " - " + this.dayKey + " " + this.timeSlot;
            }
        }
    }

    public void addDocumentWithMetadata(String documentId, String fileName, String fileType, String textContent) {
        addDocumentWithContent(documentId, fileName, fileType, textContent, textContent != null ? (long) textContent.length() : 0L);
    }

    public void removeDocument(String documentId) {
        this.documentIds.remove(documentId);
        this.documentDetails.removeIf(doc -> doc.getDocumentId().equals(documentId));
        this.updatedAt = LocalDateTime.now();
        this.stats.decrementDocumentCount();
        this.restorationData.removeDocumentFromRestoration(documentId);
    }

    public List<DocumentInfo> getDocumentsByType(String fileType) {
        return this.documentDetails.stream()
                .filter(doc -> doc.getFileType().equalsIgnoreCase(fileType))
                .toList();
    }

    // ============================================
    // MESSAGE MANAGEMENT
    // ============================================

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.stats.incrementMessageCount();

        // Update session title for first message in chat session
        if ("AI_CHAT".equals(this.sessionType) && this.messages.size() == 1) {
            String preview = message.getContent().length() > 30
                    ? message.getContent().substring(0, 30) + "..."
                    : message.getContent();
            this.sessionTitle = "Chat - " + preview + " - " + this.dayKey + " " + this.timeSlot;
        }
    }

    public void addUserMessage(String content) {
        ChatMessage userMessage = new ChatMessage("USER", content);
        addMessage(userMessage);
    }

    public void addAIMessage(String content, String metadata) {
        ChatMessage aiMessage = new ChatMessage("AI", content, metadata);
        addMessage(aiMessage);

        // Also store in AI responses for separate tracking
        AIResponse aiResponse = new AIResponse(content, metadata, LocalDateTime.now());
        this.aiResponses.add(aiResponse);
        this.restorationData.addAIResponseForRestoration(content, metadata);
    }

    public void addSystemMessage(String content) {
        ChatMessage systemMessage = new ChatMessage("SYSTEM", content);
        addMessage(systemMessage);
    }

    // ============================================
    // SEARCH MANAGEMENT
    // ============================================

    public void addSearchQuery(String query, String queryType, int resultsCount) {
        SearchQuery searchQuery = new SearchQuery(query, queryType, resultsCount, LocalDateTime.now());
        this.searchQueries.add(searchQuery);
        this.updatedAt = LocalDateTime.now();
        this.stats.incrementSearchCount();

        // Update session title for search session
        if ("SEARCH".equals(this.sessionType)) {
            this.sessionTitle = "Search - \"" + query + "\" - " + this.dayKey + " " + this.timeSlot;
        }
    }

    public List<SearchQuery> getRecentSearches(int limit) {
        return this.searchQueries.stream()
                .sorted((s1, s2) -> s2.getTimestamp().compareTo(s1.getTimestamp()))
                .limit(limit)
                .toList();
    }

    // ============================================
    // SESSION RESTORATION
    // ============================================

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void prepareForRestoration() {
        this.restorationData.prepareRestoration(this);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canBeRestored() {
        return this.isActive &&
                "ACTIVE".equals(this.status) &&
                this.restorationData != null;
    }

    // ============================================
    // SESSION ANALYTICS
    // ============================================

    public void updateSessionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Session: ").append(this.sessionType).append(" | ");
        summary.append("Documents: ").append(this.documentIds.size()).append(" | ");
        summary.append("Messages: ").append(this.messages.size()).append(" | ");
        summary.append("AI Responses: ").append(this.aiResponses.size()).append(" | ");
        summary.append("Searches: ").append(this.searchQueries.size());

        this.sessionSummary = summary.toString();
        this.updatedAt = LocalDateTime.now();
    }

    public double getSessionDurationInMinutes() {
        if (this.createdAt != null && this.lastAccessedAt != null) {
            return java.time.Duration.between(this.createdAt, this.lastAccessedAt).toMinutes();
        }
        return 0.0;
    }

    // ============================================
    // SESSION STATUS MANAGEMENT
    // ============================================

    public void activateSession() {
        this.isActive = true;
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void archiveSession() {
        this.status = "ARCHIVED";
        this.updatedAt = LocalDateTime.now();
    }

    public void deleteSession() {
        this.isActive = false;
        this.status = "DELETED";
        this.updatedAt = LocalDateTime.now();
    }

    public void restoreSession() {
        this.isActive = true;
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }

    // ============================================
    // GETTERS AND SETTERS
    // ============================================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) {
        this.userId = userId;
        generateSessionId(); // Regenerate ID when userId changes
    }

    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public String getDayKey() { return dayKey; }
    public void setDayKey(String dayKey) { this.dayKey = dayKey; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<String> documentIds) {
        this.documentIds = documentIds != null ? documentIds : new ArrayList<>();
    }

    public List<DocumentInfo> getDocumentDetails() { return documentDetails; }
    public void setDocumentDetails(List<DocumentInfo> documentDetails) {
        this.documentDetails = documentDetails != null ? documentDetails : new ArrayList<>();
    }

    public List<AIResponse> getAiResponses() { return aiResponses; }
    public void setAiResponses(List<AIResponse> aiResponses) {
        this.aiResponses = aiResponses != null ? aiResponses : new ArrayList<>();
    }

    public List<SearchQuery> getSearchQueries() { return searchQueries; }
    public void setSearchQueries(List<SearchQuery> searchQueries) {
        this.searchQueries = searchQueries != null ? searchQueries : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSessionSummary() { return sessionSummary; }
    public void setSessionSummary(String sessionSummary) { this.sessionSummary = sessionSummary; }

    public SessionStats getStats() { return stats; }
    public void setStats(SessionStats stats) { this.stats = stats != null ? stats : new SessionStats(); }

    public RestorationData getRestorationData() { return restorationData; }
    public void setRestorationData(RestorationData restorationData) {
        this.restorationData = restorationData != null ? restorationData : new RestorationData();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    public int getMessageCount() {
        return this.messages != null ? this.messages.size() : 0;
    }

    public int getDocumentCount() {
        return this.documentIds != null ? this.documentIds.size() : 0;
    }

    public int getAIResponseCount() {
        return this.aiResponses != null ? this.aiResponses.size() : 0;
    }

    public int getSearchCount() {
        return this.searchQueries != null ? this.searchQueries.size() : 0;
    }

    public boolean hasDocuments() {
        return this.documentIds != null && !this.documentIds.isEmpty();
    }

    public boolean hasMessages() {
        return this.messages != null && !this.messages.isEmpty();
    }

    public boolean hasAIResponses() {
        return this.aiResponses != null && !this.aiResponses.isEmpty();
    }

    public String getFormattedCreationTime() {
        if (this.createdAt != null) {
            return this.createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        }
        return "Unknown";
    }

    public String getFormattedLastAccess() {
        if (this.lastAccessedAt != null) {
            return this.lastAccessedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        }
        return "Never";
    }

    @Override
    public String toString() {
        return "ChatSession{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", sessionType='" + sessionType + '\'' +
                ", dayKey='" + dayKey + '\'' +
                ", timeSlot='" + timeSlot + '\'' +
                ", documentCount=" + getDocumentCount() +
                ", messageCount=" + getMessageCount() +
                ", aiResponseCount=" + getAIResponseCount() +
                ", isActive=" + isActive +
                ", status='" + status + '\'' +
                '}';
    }

    // ============================================
    // INNER CLASSES
    // ============================================

    public static class DocumentInfo {
        private String documentId;
        private String fileName;
        private String fileType;
        private long fileSize;
        private LocalDateTime uploadTime;
        private String filePath;
        private String checksum;

        // ✅ NEW: Enhanced fields for content storage
        private String textContent;    // Store actual document text
        private int contentLength;     // Length of text content
        private String contentHash;    // Hash of content for validation

        public DocumentInfo() {}

        public DocumentInfo(String documentId, String fileName, String fileType, LocalDateTime uploadTime) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.fileType = fileType;
            this.uploadTime = uploadTime;
        }

        // ✅ ENHANCED: Constructor with content
        public DocumentInfo(String documentId, String fileName, String fileType, LocalDateTime uploadTime, String textContent) {
            this(documentId, fileName, fileType, uploadTime);
            this.textContent = textContent;
            this.contentLength = textContent != null ? textContent.length() : 0;
            this.fileSize = this.contentLength;
        }

        // Getters and setters for DocumentInfo
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public LocalDateTime getUploadTime() { return uploadTime; }
        public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }

        // ✅ NEW: Content-related getters and setters
        public String getTextContent() { return textContent; }
        public void setTextContent(String textContent) {
            this.textContent = textContent;
            this.contentLength = textContent != null ? textContent.length() : 0;
            if (this.fileSize <= 0) {
                this.fileSize = this.contentLength;
            }
        }
        public int getContentLength() { return contentLength; }
        public void setContentLength(int contentLength) { this.contentLength = contentLength; }
        public String getContentHash() { return contentHash; }
        public void setContentHash(String contentHash) { this.contentHash = contentHash; }

        public boolean hasValidContent() {
            return textContent != null && textContent.trim().length() > 10;
        }
    }

    public static class AIResponse {
        private String response;
        private String metadata;
        private LocalDateTime timestamp;
        private String questionContext;
        private int documentsAnalyzed;
        private List<String> documentNames;

        public AIResponse() {}

        public AIResponse(String response, String metadata, LocalDateTime timestamp) {
            this.response = response;
            this.metadata = metadata;
            this.timestamp = timestamp;
        }

        // Getters and setters for AIResponse
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getQuestionContext() { return questionContext; }
        public void setQuestionContext(String questionContext) { this.questionContext = questionContext; }
        public int getDocumentsAnalyzed() { return documentsAnalyzed; }
        public void setDocumentsAnalyzed(int documentsAnalyzed) { this.documentsAnalyzed = documentsAnalyzed; }
        public List<String> getDocumentNames() { return documentNames; }
        public void setDocumentNames(List<String> documentNames) { this.documentNames = documentNames; }
    }

    public static class SearchQuery {
        private String query;
        private String queryType;
        private int resultsCount;
        private LocalDateTime timestamp;
        private double executionTime;

        public SearchQuery() {}

        public SearchQuery(String query, String queryType, int resultsCount, LocalDateTime timestamp) {
            this.query = query;
            this.queryType = queryType;
            this.resultsCount = resultsCount;
            this.timestamp = timestamp;
        }

        // Getters and setters for SearchQuery
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        public int getResultsCount() { return resultsCount; }
        public void setResultsCount(int resultsCount) { this.resultsCount = resultsCount; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public double getExecutionTime() { return executionTime; }
        public void setExecutionTime(double executionTime) { this.executionTime = executionTime; }
    }

    public static class SessionStats {
        private int documentCount = 0;
        private int messageCount = 0;
        private int aiResponseCount = 0;
        private int searchCount = 0;
        private LocalDateTime firstActivity;
        private LocalDateTime lastActivity;

        public SessionStats() {
            this.firstActivity = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
        }

        public void incrementDocumentCount() {
            this.documentCount++;
            updateActivity();
        }
        public void decrementDocumentCount() {
            if (this.documentCount > 0) this.documentCount--;
        }
        public void incrementMessageCount() {
            this.messageCount++;
            updateActivity();
        }
        public void incrementAiResponseCount() {
            this.aiResponseCount++;
            updateActivity();
        }
        public void incrementSearchCount() {
            this.searchCount++;
            updateActivity();
        }

        private void updateActivity() {
            this.lastActivity = LocalDateTime.now();
            if (this.firstActivity == null) {
                this.firstActivity = LocalDateTime.now();
            }
        }

        // Getters and setters for SessionStats
        public int getDocumentCount() { return documentCount; }
        public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
        public int getAiResponseCount() { return aiResponseCount; }
        public void setAiResponseCount(int aiResponseCount) { this.aiResponseCount = aiResponseCount; }
        public int getSearchCount() { return searchCount; }
        public void setSearchCount(int searchCount) { this.searchCount = searchCount; }
        public LocalDateTime getFirstActivity() { return firstActivity; }
        public void setFirstActivity(LocalDateTime firstActivity) { this.firstActivity = firstActivity; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    }

    public static class RestorationData {
        private Map<String, Object> documentRestorationData = new HashMap<>();
        private Map<String, Object> chatRestorationData = new HashMap<>();
        private Map<String, Object> searchRestorationData = new HashMap<>();
        private LocalDateTime restorationPreparedAt;

        // ✅ ENHANCED: Store documents with content for restoration
        public void addDocumentForRestoration(String documentId, String fileName, String fileType, String textContent) {
            Map<String, Object> docData = new HashMap<>();
            docData.put("documentId", documentId);
            docData.put("fileName", fileName);
            docData.put("fileType", fileType);
            docData.put("textContent", textContent); // ✅ Store content
            docData.put("contentLength", textContent != null ? textContent.length() : 0);
            docData.put("timestamp", LocalDateTime.now());
            documentRestorationData.put(documentId, docData);
        }

        // ✅ BACKWARD COMPATIBILITY: Old method without content
        public void addDocumentForRestoration(String documentId, String fileName, String fileType) {
            addDocumentForRestoration(documentId, fileName, fileType, null);
        }

        public void removeDocumentFromRestoration(String documentId) {
            documentRestorationData.remove(documentId);
        }

        public void addAIResponseForRestoration(String response, String metadata) {
            String key = "ai_" + System.currentTimeMillis();
            Map<String, Object> aiData = new HashMap<>();
            aiData.put("response", response);
            aiData.put("metadata", metadata);
            aiData.put("timestamp", LocalDateTime.now());
            chatRestorationData.put(key, aiData);
        }

        public void prepareRestoration(ChatSession session) {
            this.restorationPreparedAt = LocalDateTime.now();
            // Prepare any additional restoration data if needed
        }

        // Getters and setters for RestorationData
        public Map<String, Object> getDocumentRestorationData() { return documentRestorationData; }
        public void setDocumentRestorationData(Map<String, Object> documentRestorationData) {
            this.documentRestorationData = documentRestorationData != null ? documentRestorationData : new HashMap<>();
        }
        public Map<String, Object> getChatRestorationData() { return chatRestorationData; }
        public void setChatRestorationData(Map<String, Object> chatRestorationData) {
            this.chatRestorationData = chatRestorationData != null ? chatRestorationData : new HashMap<>();
        }
        public Map<String, Object> getSearchRestorationData() { return searchRestorationData; }
        public void setSearchRestorationData(Map<String, Object> searchRestorationData) {
            this.searchRestorationData = searchRestorationData != null ? searchRestorationData : new HashMap<>();
        }
        public LocalDateTime getRestorationPreparedAt() { return restorationPreparedAt; }
        public void setRestorationPreparedAt(LocalDateTime restorationPreparedAt) {
            this.restorationPreparedAt = restorationPreparedAt;
        }
    }
}
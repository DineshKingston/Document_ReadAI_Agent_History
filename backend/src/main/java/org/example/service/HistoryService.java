package org.example.service;

import org.example.model.*;
import org.example.repository.ChatSessionRepository;
import org.example.repository.DocumentSessionRepository;
import org.example.repository.ChatMessageRepository;
import org.example.repository.SearchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private DocumentSessionRepository documentSessionRepository;

    // ✅ ADD: Missing repository injections for chat and search history
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    // ✅ ADD: Track current session for each user
    private final Map<String, String> userCurrentSessionMap = new ConcurrentHashMap<>();

    // ✅ ADD: Get current session ID for user
    private String getCurrentSessionId(String userId) {
        return userCurrentSessionMap.get(userId);
    }

    // ✅ ADD: Set current session for user
    public void setCurrentSession(String userId, String sessionId) {
        userCurrentSessionMap.put(userId, sessionId);
        System.out.println("✅ Set current session for user " + userId + ": " + sessionId);
    }

    // ============================================
    // UNIFIED SESSION MANAGEMENT - ONE SESSION FOR ALL ACTIVITIES
    // ============================================

    public ChatSession createNewDaySession(String userId, String sessionType) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String timeSlot = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            String sessionTitle;
            if ("UNIFIED_SESSION".equals(sessionType)) {
                sessionTitle = "🎯 Work Session - " + today + " " + timeSlot;
            } else {
                sessionTitle = generateSessionTitle(sessionType, today, timeSlot);
            }

            ChatSession newSession = new ChatSession(userId, sessionTitle, sessionType);

            if ("UNIFIED_SESSION".equals(sessionType)) {
                newSession.addSystemMessage("🎯 Work Session Started - All documents, searches, and AI chats will be stored in this single session.");
            }

            ChatSession savedSession = chatSessionRepository.save(newSession);

            // ✅ Set as current session for user
            setCurrentSession(userId, savedSession.getId());

            System.out.println("✅ Created new unified session: " + savedSession.getId() +
                    " for user: " + userId + " on day: " + today + " type: " + sessionType);

            return savedSession;
        } catch (Exception e) {
            System.err.println("❌ Error creating unified session: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create unified session: " + e.getMessage());
        }
    }

    public ChatSession getCurrentOrCreateTodaySession(String userId, String requestedType) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            List<ChatSession> unifiedSessions = chatSessionRepository
                    .findByUserIdAndDayKeyAndSessionTypeAndIsActiveTrueOrderByCreatedAtDesc(userId, today, "UNIFIED_SESSION");

            if (!unifiedSessions.isEmpty()) {
                ChatSession unifiedSession = unifiedSessions.get(0);
                unifiedSession.updateLastAccessed();
                ChatSession savedSession = chatSessionRepository.save(unifiedSession);

                // ✅ Set as current session for user
                setCurrentSession(userId, savedSession.getId());

                System.out.println("✅ Using existing unified session: " + savedSession.getId() + " for user: " + userId);
                return savedSession;
            }

            System.out.println("🆕 Creating new unified session for user: " + userId + " (requested: " + requestedType + ")");
            return createNewDaySession(userId, "UNIFIED_SESSION");

        } catch (Exception e) {
            System.err.println("❌ Error getting/creating unified session: " + e.getMessage());
            e.printStackTrace();
            return createNewDaySession(userId, "UNIFIED_SESSION");
        }
    }

    public ChatSession addDocumentToTodaySession(String userId, String documentId, String fileName, String fileType, String textContent, Long fileSize) {
        try {
            ChatSession currentSession = getCurrentOrCreateTodaySession(userId, "UNIFIED_SESSION");
            currentSession.addDocumentWithContent(documentId, fileName, fileType, textContent, fileSize);
            ChatSession savedSession = chatSessionRepository.save(currentSession);
            System.out.println("✅ Added document with " + (textContent != null ? textContent.length() : 0) + " characters of content");
            return savedSession;
        } catch (Exception e) {
            System.err.println("❌ Error adding document with content: " + e.getMessage());
            throw new RuntimeException("Failed to add document with content: " + e.getMessage());
        }
    }

    // ✅ ENHANCED: Save AI messages to both session and detailed chat history
    public ChatSession addAiMessageToSession(String userId, String question, String aiResponse, String metadata) {
        try {
            ChatSession currentSession = getCurrentOrCreateTodaySession(userId, "UNIFIED_SESSION");

            // Add to session
            currentSession.addUserMessage(question);
            currentSession.addAIMessage(aiResponse, metadata);

            // ✅ Also save to detailed chat message collection
            saveAIChatMessage(userId, question, aiResponse, metadata);

            ChatSession savedSession = chatSessionRepository.save(currentSession);
            System.out.println("✅ Added AI conversation to unified session: " + savedSession.getId());
            return savedSession;
        } catch (Exception e) {
            System.err.println("❌ Error adding AI message to unified session: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add AI message to unified session: " + e.getMessage());
        }
    }

    public ChatSession addSearchToSession(String userId, String query, String queryType, int resultsCount) {
        try {
            ChatSession currentSession = getCurrentOrCreateTodaySession(userId, "UNIFIED_SESSION");
            currentSession.addSearchQuery(query, queryType, resultsCount);

            // ✅ Also save to detailed search history collection
            saveSearchQuery(userId, query, resultsCount, queryType);

            ChatSession savedSession = chatSessionRepository.save(currentSession);
            System.out.println("✅ Added search query to unified session: " + savedSession.getId());
            return savedSession;
        } catch (Exception e) {
            System.err.println("❌ Error adding search to unified session: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add search to unified session: " + e.getMessage());
        }
    }

    // ✅ COMPLETE: Save AI chat messages with proper session tracking
    public void saveAIChatMessage(String userId, String question, String aiResponse, String metadata) {
        try {
            String sessionId = getCurrentSessionId(userId);
            if (sessionId == null) {
                System.err.println("⚠️ No current session for user: " + userId + ", cannot save chat message");
                return;
            }

            // Save user question
            ChatMessage userMessage = new ChatMessage();
            userMessage.setUserId(userId);
            userMessage.setSessionId(sessionId);
            userMessage.setType("USER");
            userMessage.setContent(question);
            userMessage.setTimestamp(LocalDateTime.now());
            userMessage.setMetadata(metadata);

            // Save AI response (slightly delayed timestamp for proper ordering)
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setUserId(userId);
            aiMessage.setSessionId(sessionId);
            aiMessage.setType("AI");
            aiMessage.setContent(aiResponse);
            aiMessage.setTimestamp(LocalDateTime.now().plusSeconds(1));
            aiMessage.setMetadata(metadata);

            // Save both messages
            chatMessageRepository.save(userMessage);
            chatMessageRepository.save(aiMessage);

            System.out.println("✅ Saved chat exchange: " + question.substring(0, Math.min(50, question.length())));

        } catch (Exception e) {
            System.err.println("❌ Failed to save chat messages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ COMPLETE: Save search queries with proper session tracking
    public void saveSearchQuery(String userId, String query, int resultsCount, String queryType) {
        try {
            String sessionId = getCurrentSessionId(userId);
            if (sessionId == null) {
                System.err.println("⚠️ No current session for user: " + userId + ", cannot save search query");
                return;
            }

            SearchHistory searchEntry = new SearchHistory();
            searchEntry.setUserId(userId);
            searchEntry.setSessionId(sessionId);
            searchEntry.setQuery(query);
            searchEntry.setResultsCount(resultsCount);
            searchEntry.setQueryType(queryType);
            searchEntry.setTimestamp(LocalDateTime.now());

            searchHistoryRepository.save(searchEntry);
            System.out.println("✅ Saved search query: " + query);

        } catch (Exception e) {
            System.err.println("❌ Failed to save search query: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ COMPLETE: Get complete session history including chat and search
    public Map<String, Object> getCompleteSessionHistory(String sessionId) {
        Map<String, Object> sessionData = new HashMap<>();

        try {
            // Get detailed chat messages
            List<ChatMessage> chatMessages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
            sessionData.put("chatMessages", chatMessages);

            // Get detailed search history
            List<SearchHistory> searchHistory = searchHistoryRepository.findBySessionIdOrderByTimestampAsc(sessionId);
            sessionData.put("searchHistory", searchHistory);

            // Get session and documents
            ChatSession session = getChatSessionById(sessionId);
            if (session != null) {
                sessionData.put("documents", session.getDocumentDetails());
                sessionData.put("session", session);
            }

            System.out.println("✅ Retrieved complete session history for " + sessionId + ": " +
                    chatMessages.size() + " chat messages, " +
                    searchHistory.size() + " searches");

            return sessionData;

        } catch (Exception e) {
            System.err.println("❌ Failed to get complete session history: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ============================================
    // SESSION RETRIEVAL BY DAY - UNIFIED SESSIONS ONLY
    // ============================================

    public Map<String, Object> getDayWiseHistory(String userId) {
        try {
            List<ChatSession> allSessions = chatSessionRepository
                    .findByUserIdAndSessionTypeAndIsActiveTrueOrderByDayKeyDescCreatedAtDesc(userId, "UNIFIED_SESSION");

            Map<String, List<ChatSession>> sessionsByDay = allSessions.stream()
                    .collect(Collectors.groupingBy(ChatSession::getDayKey,
                            LinkedHashMap::new,
                            Collectors.toList()));

            sessionsByDay.forEach((day, sessions) -> {
                sessions.sort((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()));
            });

            Map<String, Object> result = new HashMap<>();
            result.put("sessionsByDay", sessionsByDay);
            result.put("totalDays", sessionsByDay.size());
            result.put("totalSessions", allSessions.size());

            Map<String, Object> stats = calculateUnifiedSessionStatistics(allSessions);
            result.put("statistics", stats);

            List<String> uniqueDays = new ArrayList<>(sessionsByDay.keySet());
            result.put("availableDays", uniqueDays);

            System.out.println("✅ Retrieved unified session history for user: " + userId +
                    " (" + allSessions.size() + " unified sessions across " + sessionsByDay.size() + " days)");

            return result;
        } catch (Exception e) {
            System.err.println("❌ Error getting unified session history: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public List<ChatSession> getSessionsForDay(String userId, String dayKey) {
        try {
            List<ChatSession> sessions = chatSessionRepository
                    .findByUserIdAndDayKeyAndSessionTypeAndIsActiveTrueOrderByCreatedAtDesc(userId, dayKey, "UNIFIED_SESSION");

            System.out.println("✅ Retrieved " + sessions.size() + " unified sessions for user: " + userId + " on day: " + dayKey);
            return sessions;
        } catch (Exception e) {
            System.err.println("❌ Error getting unified sessions for day: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ✅ ENHANCED: Restore session with session tracking
    public ChatSession restoreSession(String sessionId) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isPresent()) {
                ChatSession session = sessionOpt.get();
                session.updateLastAccessed();
                session.prepareForRestoration();
                ChatSession savedSession = chatSessionRepository.save(session);

                // ✅ Set as current session for user
                setCurrentSession(session.getUserId(), sessionId);

                System.out.println("✅ Restored unified session: " + sessionId +
                        " with " + session.getDocumentCount() + " documents, " +
                        session.getMessageCount() + " messages, and " +
                        session.getAIResponseCount() + " AI responses");

                return savedSession;
            } else {
                System.err.println("❌ Unified session not found for restoration: " + sessionId);
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error restoring unified session: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ============================================
    // SESSION ANALYTICS & SEARCH - UNIFIED SESSIONS
    // ============================================

    public List<ChatSession> searchSessions(String userId, String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return chatSessionRepository.findByUserIdAndSessionTypeAndIsActiveTrueOrderByDayKeyDescCreatedAtDesc(userId, "UNIFIED_SESSION");
            }

            List<ChatSession> titleResults = chatSessionRepository
                    .findByUserIdAndSessionTitleContainingIgnoreCaseAndIsActiveTrue(userId, searchTerm)
                    .stream()
                    .filter(session -> "UNIFIED_SESSION".equals(session.getSessionType()))
                    .collect(Collectors.toList());

            System.out.println("✅ Found " + titleResults.size() + " unified sessions matching search: " + searchTerm);
            return titleResults;
        } catch (Exception e) {
            System.err.println("❌ Error searching unified sessions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<ChatSession> getRecentSessions(String userId, int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<ChatSession> recentSessions = chatSessionRepository
                    .findRecentlyAccessedSessions(userId, since)
                    .stream()
                    .filter(session -> "UNIFIED_SESSION".equals(session.getSessionType()))
                    .collect(Collectors.toList());

            System.out.println("✅ Retrieved " + recentSessions.size() + " recent unified sessions (last " + days + " days)");
            return recentSessions;
        } catch (Exception e) {
            System.err.println("❌ Error getting recent unified sessions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<ChatSession> getSessionsByType(String userId, String sessionType) {
        try {
            List<ChatSession> typedSessions = chatSessionRepository
                    .findByUserIdAndSessionTypeAndIsActiveTrueOrderByDayKeyDescCreatedAtDesc(userId, "UNIFIED_SESSION");

            System.out.println("✅ Retrieved " + typedSessions.size() + " unified sessions");
            return typedSessions;
        } catch (Exception e) {
            System.err.println("❌ Error getting unified sessions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================
    // SESSION MANAGEMENT
    // ============================================

    public boolean deleteSession(String sessionId) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isPresent()) {
                ChatSession session = sessionOpt.get();
                session.deleteSession();
                chatSessionRepository.save(session);

                // ✅ Also delete detailed chat messages and search history
                chatMessageRepository.deleteBySessionId(sessionId);
                searchHistoryRepository.deleteBySessionId(sessionId);

                System.out.println("✅ Deleted unified session: " + sessionId);
                return true;
            } else {
                System.err.println("❌ Unified session not found for deletion: " + sessionId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error deleting unified session: " + e.getMessage());
            return false;
        }
    }

    public boolean archiveSession(String sessionId) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isPresent()) {
                ChatSession session = sessionOpt.get();
                session.archiveSession();
                chatSessionRepository.save(session);
                System.out.println("✅ Archived unified session: " + sessionId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error archiving unified session: " + e.getMessage());
            return false;
        }
    }

    public boolean clearUserHistory(String userId) {
        try {
            List<ChatSession> allSessions = chatSessionRepository.findByUserIdAndIsActiveTrue(userId);
            for (ChatSession session : allSessions) {
                session.deleteSession();
                // Also clear detailed history
                chatMessageRepository.deleteBySessionId(session.getId());
                searchHistoryRepository.deleteBySessionId(session.getId());
            }
            chatSessionRepository.saveAll(allSessions);
            System.out.println("✅ Cleared unified session history for user: " + userId + " (" + allSessions.size() + " sessions)");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error clearing user history: " + e.getMessage());
            return false;
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    private String generateSessionTitle(String sessionType, String dayKey, String timeSlot) {
        return switch (sessionType) {
            case "UNIFIED_SESSION" -> "🎯 Unified Work Session - " + dayKey + " " + timeSlot;
            case "DOCUMENT_ANALYSIS" -> "Documents - " + dayKey + " " + timeSlot;
            case "AI_CHAT" -> "AI Chat - " + dayKey + " " + timeSlot;
            case "SEARCH" -> "Search - " + dayKey + " " + timeSlot;
            default -> "Session - " + dayKey + " " + timeSlot;
        };
    }

    private Map<String, Object> calculateUnifiedSessionStatistics(List<ChatSession> sessions) {
        Map<String, Object> stats = new HashMap<>();

        long totalDocuments = sessions.stream().mapToLong(ChatSession::getDocumentCount).sum();
        long totalMessages = sessions.stream().mapToLong(ChatSession::getMessageCount).sum();
        long totalAIResponses = sessions.stream().mapToLong(ChatSession::getAIResponseCount).sum();
        long totalSearches = sessions.stream().mapToLong(ChatSession::getSearchCount).sum();

        Set<String> uniqueDays = sessions.stream().map(ChatSession::getDayKey).collect(Collectors.toSet());

        stats.put("unifiedSessions", sessions.size());
        stats.put("totalDocuments", totalDocuments);
        stats.put("totalMessages", totalMessages);
        stats.put("totalAIResponses", totalAIResponses);
        stats.put("totalSearches", totalSearches);
        stats.put("activeDays", uniqueDays.size());
        stats.put("totalSessions", sessions.size());

        return stats;
    }

    public ChatSession getChatSessionById(String sessionId) {
        try {
            return chatSessionRepository.findById(sessionId).orElse(null);
        } catch (Exception e) {
            System.err.println("❌ Error getting session by ID: " + e.getMessage());
            return null;
        }
    }

    // Backward compatibility methods
    public ChatSession createNewChatSession(String userId, String sessionTitle, String sessionType) {
        return createNewDaySession(userId, "UNIFIED_SESSION");
    }

    public List<ChatSession> getUserChatHistory(String userId) {
        return chatSessionRepository.findByUserIdAndSessionTypeAndIsActiveTrueOrderByDayKeyDescCreatedAtDesc(userId, "UNIFIED_SESSION");
    }
}

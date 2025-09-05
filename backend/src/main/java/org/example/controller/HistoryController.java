package org.example.controller;

import org.example.model.ChatSession;
import org.example.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    // ============================================
    // DAY-WISE SESSION ENDPOINTS
    // ============================================

    @PostMapping("/session/new")
    public ResponseEntity<Map<String, Object>> createNewSession(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = (String) request.get("userId");
            String sessionType = (String) request.getOrDefault("sessionType", "UNIFIED_SESSION"); // Changed default

            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "User ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Create unified session that handles all activities
            ChatSession newSession = historyService.createNewDaySession(userId, sessionType);

            response.put("success", true);
            response.put("session", newSession);
            response.put("message", "New unified session created");
            response.put("sessionId", newSession.getId());
            response.put("dayKey", newSession.getDayKey());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error creating unified session: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    // ✅ ADD to HistoryController.java
    @RequestMapping(method = RequestMethod.OPTIONS, value = "/**")
    public ResponseEntity<Void> handleOptionsHistory() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD")
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }


    // ✅ FIXED: HistoryController.java - Updated addDocument method
    @PostMapping("/document/add")
    public ResponseEntity<Map<String, Object>> addDocument(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = (String) request.get("userId");
            String documentId = (String) request.get("documentId");
            String fileName = (String) request.get("fileName");
            String fileType = (String) request.get("fileType");

            // ✅ NEW: Extract textContent and fileSize from request
            String textContent = (String) request.getOrDefault("textContent", "");
            Long fileSize = null;

            // Handle fileSize conversion from different types
            Object sizeObj = request.get("fileSize");
            if (sizeObj instanceof Number) {
                fileSize = ((Number) sizeObj).longValue();
            } else if (sizeObj instanceof String) {
                try {
                    fileSize = Long.parseLong((String) sizeObj);
                } catch (NumberFormatException e) {
                    fileSize = 0L;
                }
            } else {
                fileSize = textContent != null ? (long) textContent.length() : 0L;
            }

            if (userId == null || documentId == null || fileName == null) {
                response.put("success", false);
                response.put("error", "UserId, documentId, and fileName are required");
                return ResponseEntity.badRequest().body(response);
            }

            // ✅ FIXED: Call method with all 6 parameters
            ChatSession updatedSession = historyService.addDocumentToTodaySession(
                    userId, documentId, fileName, fileType, textContent, fileSize);

            response.put("success", true);
            response.put("session", updatedSession);
            response.put("message", "Document added to today's session with content");
            response.put("sessionId", updatedSession.getId());
            response.put("documentCount", updatedSession.getDocumentCount());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error adding document: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    @PostMapping("/ai/message")
    public ResponseEntity<Map<String, Object>> addAiMessage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = (String) request.get("userId");
            String question = (String) request.get("question");
            String aiResponse = (String) request.get("aiResponse");
            String metadata = (String) request.get("metadata");

            if (userId == null || question == null || aiResponse == null) {
                response.put("success", false);
                response.put("error", "UserId, question, and aiResponse are required");
                return ResponseEntity.badRequest().body(response);
            }

            ChatSession updatedSession = historyService.addAiMessageToSession(userId, question, aiResponse, metadata);

            response.put("success", true);
            response.put("session", updatedSession);
            response.put("message", "AI conversation saved");
            response.put("sessionId", updatedSession.getId());
            response.put("messageCount", updatedSession.getMessageCount());
            response.put("aiResponseCount", updatedSession.getAIResponseCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error saving AI message: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/search/add")
    public ResponseEntity<Map<String, Object>> addSearch(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = (String) request.get("userId");
            String query = (String) request.get("query");
            String queryType = (String) request.getOrDefault("queryType", "KEYWORD");
            Integer resultsCount = (Integer) request.getOrDefault("resultsCount", 0);

            if (userId == null || query == null) {
                response.put("success", false);
                response.put("error", "UserId and query are required");
                return ResponseEntity.badRequest().body(response);
            }

            ChatSession updatedSession = historyService.addSearchToSession(userId, query, queryType, resultsCount);

            response.put("success", true);
            response.put("session", updatedSession);
            response.put("message", "Search query saved");
            response.put("sessionId", updatedSession.getId());
            response.put("searchCount", updatedSession.getSearchCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error saving search: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // SESSION RETRIEVAL ENDPOINTS
    // ============================================

    @GetMapping("/daywise/{userId}")
    public ResponseEntity<Map<String, Object>> getDayWiseHistory(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> history = historyService.getDayWiseHistory(userId);
            response.put("success", true);
            response.putAll(history);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error fetching day-wise history: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/day/{userId}/{dayKey}")
    public ResponseEntity<Map<String, Object>> getSessionsForDay(@PathVariable String userId, @PathVariable String dayKey) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ChatSession> sessions = historyService.getSessionsForDay(userId, dayKey);
            response.put("success", true);
            response.put("sessions", sessions);
            response.put("dayKey", dayKey);
            response.put("sessionCount", sessions.size());

            // Group by session type
            Map<String, Long> sessionsByType = sessions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            ChatSession::getSessionType,
                            java.util.stream.Collectors.counting()
                    ));
            response.put("sessionsByType", sessionsByType);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error fetching day sessions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ✅ ENHANCED: Get complete session data including all history
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ChatSession session = historyService.getChatSessionById(sessionId);
            if (session != null) {
                response.put("success", true);
                response.put("session", session);

                // ✅ NEW: Include complete historical data
                response.put("documents", session.getDocumentDetails());
                response.put("messages", session.getMessages());
                response.put("aiResponses", session.getAiResponses());
                response.put("searchQueries", session.getSearchQueries());
                response.put("stats", session.getStats());
                response.put("restorationData", session.getRestorationData());

                // ✅ NEW: Add summary statistics
                Map<String, Object> historySummary = new HashMap<>();
                historySummary.put("totalMessages", session.getMessageCount());
                historySummary.put("totalAIResponses", session.getAIResponseCount());
                historySummary.put("totalSearches", session.getSearchCount());
                historySummary.put("totalDocuments", session.getDocumentCount());
                historySummary.put("sessionDuration", session.getSessionDurationInMinutes());
                historySummary.put("lastAccessed", session.getLastAccessedAt());

                response.put("historySummary", historySummary);

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Session not found");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error fetching session: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    // ✅ NEW: Get complete session history endpoint
    @GetMapping("/session/complete/{sessionId}")
    public ResponseEntity<Map<String, Object>> getCompleteSessionHistory(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("📋 Getting complete history for session: " + sessionId);

            Map<String, Object> sessionData = historyService.getCompleteSessionHistory(sessionId);

            if (!sessionData.isEmpty()) {
                response.put("success", true);
                response.putAll(sessionData);

                @SuppressWarnings("unchecked")
                List<Object> chatMessages = (List<Object>) sessionData.get("chatMessages");
                @SuppressWarnings("unchecked")
                List<Object> searchHistory = (List<Object>) sessionData.get("searchHistory");

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalChatMessages", chatMessages != null ? chatMessages.size() : 0);
                stats.put("totalSearches", searchHistory != null ? searchHistory.size() : 0);
                stats.put("restoredAt", LocalDateTime.now());

                response.put("restorationStats", stats);

                System.out.println("✅ Complete session history retrieved successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Session history not found");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("❌ Error getting complete session history: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Error fetching complete session history: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }



    @PostMapping("/session/{sessionId}/restore")
    public ResponseEntity<Map<String, Object>> restoreSession(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ChatSession session = historyService.restoreSession(sessionId);
            if (session != null) {
                response.put("success", true);
                response.put("session", session);
                response.put("documents", session.getDocumentDetails());
                response.put("messages", session.getMessages());
                response.put("aiResponses", session.getAiResponses());
                response.put("restorationData", session.getRestorationData());
                response.put("message", "Session restored successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Session not found or cannot be restored");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error restoring session: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // SESSION MANAGEMENT ENDPOINTS
    // ============================================

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = historyService.deleteSession(sessionId);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Session deleted successfully");
            } else {
                response.put("success", false);
                response.put("error", "Failed to delete session or session not found");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error deleting session: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/session/{sessionId}/archive")
    public ResponseEntity<Map<String, Object>> archiveSession(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean archived = historyService.archiveSession(sessionId);
            if (archived) {
                response.put("success", true);
                response.put("message", "Session archived successfully");
            } else {
                response.put("success", false);
                response.put("error", "Failed to archive session or session not found");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error archiving session: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // SEARCH & FILTER ENDPOINTS
    // ============================================

    @GetMapping("/search/{userId}")
    public ResponseEntity<Map<String, Object>> searchSessions(
            @PathVariable String userId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sessionType) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ChatSession> sessions;

            if (sessionType != null && !sessionType.trim().isEmpty()) {
                sessions = historyService.getSessionsByType(userId, sessionType);
            } else if (query != null && !query.trim().isEmpty()) {
                sessions = historyService.searchSessions(userId, query);
            } else {
                sessions = historyService.getUserChatHistory(userId);
            }

            response.put("success", true);
            response.put("sessions", sessions);
            response.put("totalResults", sessions.size());
            response.put("searchQuery", query);
            response.put("sessionType", sessionType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error searching sessions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/recent/{userId}")
    public ResponseEntity<Map<String, Object>> getRecentSessions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ChatSession> recentSessions = historyService.getRecentSessions(userId, days);
            response.put("success", true);
            response.put("sessions", recentSessions);
            response.put("days", days);
            response.put("totalResults", recentSessions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error fetching recent sessions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<Map<String, Object>> clearUserHistory(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean cleared = historyService.clearUserHistory(userId);
            if (cleared) {
                response.put("success", true);
                response.put("message", "User history cleared successfully");
            } else {
                response.put("success", false);
                response.put("error", "Failed to clear user history");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error clearing user history: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // BACKWARD COMPATIBILITY ENDPOINTS
    // ============================================

    @PostMapping("/chat/new")
    public ResponseEntity<Map<String, Object>> createNewChatSession(@RequestBody Map<String, Object> request) {
        return createNewSession(request);
    }

    @PostMapping("/documents/add")
    public ResponseEntity<Map<String, Object>> addDocumentToSession(@RequestBody Map<String, Object> request) {
        return addDocument(request);
    }

    @PostMapping("/chat/message")
    public ResponseEntity<Map<String, Object>> addMessageToChat(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = (String) request.get("userId");
            String messageType = (String) request.get("messageType");
            String content = (String) request.get("content");
            String metadata = (String) request.get("metadata");

            if ("AI".equals(messageType) && request.containsKey("question")) {
                String question = (String) request.get("question");
                return addAiMessage(Map.of(
                        "userId", userId,
                        "question", question,
                        "aiResponse", content,
                        "metadata", metadata
                ));
            } else {
                ChatSession session = historyService.getCurrentOrCreateTodaySession(userId, "AI_CHAT");
                if ("USER".equals(messageType)) {
                    session.addUserMessage(content);
                } else if ("SYSTEM".equals(messageType)) {
                    session.addSystemMessage(content);
                }

                response.put("success", true);
                response.put("session", session);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error adding message: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/complete/{userId}")
    public ResponseEntity<Map<String, Object>> getCompleteHistory(@PathVariable String userId) {
        return getDayWiseHistory(userId);
    }

    @GetMapping("/chat/{userId}")
    public ResponseEntity<Map<String, Object>> getChatHistory(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ChatSession> chatHistory = historyService.getUserChatHistory(userId);
            response.put("success", true);
            response.put("chatHistory", chatHistory);
            response.put("totalSessions", chatHistory.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error fetching chat history: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

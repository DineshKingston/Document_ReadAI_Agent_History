package org.example.repository;

import org.example.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    // Day-wise session queries
    List<ChatSession> findByUserIdAndDayKeyAndIsActiveTrueOrderByCreatedAtDesc(String userId, String dayKey);

    // Find all sessions for a user ordered by day and time (most recent first)
    List<ChatSession> findByUserIdAndIsActiveTrueOrderByDayKeyDescCreatedAtDesc(String userId);

    // Find sessions by type for a specific day
    List<ChatSession> findByUserIdAndDayKeyAndSessionTypeAndIsActiveTrueOrderByCreatedAtDesc(String userId, String dayKey, String sessionType);

    // Find sessions by type across all days
    List<ChatSession> findByUserIdAndSessionTypeAndIsActiveTrueOrderByDayKeyDescCreatedAtDesc(String userId, String sessionType);

    // Get sessions for date range
    @Query("{'userId': ?0, 'dayKey': {$gte: ?1, $lte: ?2}, 'isActive': true}")
    List<ChatSession> findByUserIdAndDateRangeOrderByDayKeyDescCreatedAtDesc(String userId, String startDate, String endDate);

    // Find sessions with documents
    @Query("{'userId': ?0, 'documentIds': {$exists: true, $ne: []}, 'isActive': true}")
    List<ChatSession> findSessionsWithDocumentsByUserId(String userId);

    // Find sessions with AI responses
    @Query("{'userId': ?0, 'aiResponses': {$exists: true, $ne: []}, 'isActive': true}")
    List<ChatSession> findSessionsWithAIResponsesByUserId(String userId);

    // Find sessions with messages
    @Query("{'userId': ?0, 'messages': {$exists: true, $ne: []}, 'isActive': true}")
    List<ChatSession> findSessionsWithMessagesByUserId(String userId);

    // Count sessions by day
    long countByUserIdAndDayKeyAndIsActiveTrue(String userId, String dayKey);

    // Count sessions by type
    long countByUserIdAndSessionTypeAndIsActiveTrue(String userId, String sessionType);

    // Get unique days for user (for calendar view)
    @Query(value = "{'userId': ?0, 'isActive': true}", fields = "{'dayKey': 1, '_id': 0}")
    List<String> findDistinctDayKeysByUserId(String userId);

    // Find most recent session of any type for user
    Optional<ChatSession> findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(String userId);

    // Find most recent session of specific type for user
    Optional<ChatSession> findTopByUserIdAndSessionTypeAndIsActiveTrueOrderByUpdatedAtDesc(String userId, String sessionType);

    // Find sessions created after a certain time
    List<ChatSession> findByUserIdAndCreatedAtAfterAndIsActiveTrueOrderByCreatedAtDesc(String userId, LocalDateTime after);

    // Find sessions updated after a certain time
    List<ChatSession> findByUserIdAndUpdatedAtAfterAndIsActiveTrueOrderByUpdatedAtDesc(String userId, LocalDateTime after);

    // Custom query to find sessions with activity in last N days
    @Query("{'userId': ?0, 'isActive': true, 'lastAccessedAt': {$gte: ?1}}")
    List<ChatSession> findRecentlyAccessedSessions(String userId, LocalDateTime since);

    // Find sessions by status
    List<ChatSession> findByUserIdAndStatusAndIsActiveTrueOrderByUpdatedAtDesc(String userId, String status);

    // Search sessions by title
    @Query("{'userId': ?0, 'sessionTitle': {$regex: ?1, $options: 'i'}, 'isActive': true}")
    List<ChatSession> findByUserIdAndSessionTitleContainingIgnoreCaseAndIsActiveTrue(String userId, String titlePart);

    List<ChatSession> findByUserIdAndIsActiveTrue(String userId);
}
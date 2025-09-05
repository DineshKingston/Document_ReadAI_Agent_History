package org.example.repository;

import org.example.model.DocumentSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentSessionRepository extends MongoRepository<DocumentSession, String> {

    // Find sessions by user ID and day
    List<DocumentSession> findByUserIdAndDayKeyAndIsActiveTrueOrderByCreatedAtDesc(String userId, String dayKey);

    // Find sessions by user ID (all days)
    List<DocumentSession> findByUserIdAndIsActiveTrueOrderByDayKeyDescCreatedAtDesc(String userId);

    // Find session by chat session ID
    Optional<DocumentSession> findByChatSessionIdAndIsActiveTrue(String chatSessionId);

    // Find sessions with specific document
    @Query("{'userId': ?0, 'documentIds': {$in: [?1]}, 'isActive': true}")
    List<DocumentSession> findByUserIdAndDocumentId(String userId, String documentId);

    // Find sessions created after a certain date
    List<DocumentSession> findByUserIdAndCreatedAtAfterAndIsActiveTrueOrderByCreatedAtDesc(String userId, LocalDateTime after);

    // Find most recent session
    Optional<DocumentSession> findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(String userId);

    // Count sessions by day
    long countByUserIdAndDayKeyAndIsActiveTrue(String userId, String dayKey);

    // Get unique days
    @Query(value = "{'userId': ?0, 'isActive': true}", fields = "{'dayKey': 1, '_id': 0}")
    List<String> findDistinctDayKeysByUserId(String userId);
}

package org.example.repository;

import org.example.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    List<ChatMessage> findByUserIdOrderByTimestampDesc(String userId);
    List<ChatMessage> findByUserIdAndSessionId(String userId, String sessionId);
    void deleteBySessionId(String sessionId);
}

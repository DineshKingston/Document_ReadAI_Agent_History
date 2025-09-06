package org.example.repository;

import org.example.model.SearchHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SearchHistoryRepository extends MongoRepository<SearchHistory, String> {
    List<SearchHistory> findBySessionIdOrderByTimestampAsc(String sessionId);
    List<SearchHistory> findByUserIdOrderByTimestampDesc(String userId);
    List<SearchHistory> findByUserIdAndSessionId(String userId, String sessionId);
    void deleteBySessionId(String sessionId);
}
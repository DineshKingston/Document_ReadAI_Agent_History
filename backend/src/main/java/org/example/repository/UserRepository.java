package org.example.repository;

import org.example.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends MongoRepository<User, String> { // Changed from Long to String

    // Find by username (same method signature)
    Optional<User> findByUsername(String username);

    // MongoDB-specific methods
    boolean existsByUsername(String username);

    // Custom MongoDB query
    @Query("{'username': ?0}")
    Optional<User> findUserByUsername(String username);

    // Find users created after a certain date
    List<User> findByCreatedAtAfter(LocalDateTime date);

    // Find users by username containing (case insensitive)
    List<User> findByUsernameContainingIgnoreCase(String username);
}


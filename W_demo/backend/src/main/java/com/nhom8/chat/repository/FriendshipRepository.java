package com.nhom8.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom8.chat.entity.Friendship;
import com.nhom8.chat.entity.enums.FriendshipStatus;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1.id = :user1Id AND f.user2.id = :user2Id) OR " +
           "(f.user1.id = :user2Id AND f.user2.id = :user1Id)")
    Optional<Friendship> findFriendshipBetweenUsers(@Param("user1Id") Long user1Id, 
                                                   @Param("user2Id") Long user2Id);

    @Query("SELECT f FROM Friendship f WHERE f.user1.id = :userId OR f.user2.id = :userId")
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE (f.user1.id = :userId OR f.user2.id = :userId) AND f.status = :status")
    List<Friendship> findByUserIdAndStatus(@Param("userId") Long userId, 
                                          @Param("status") FriendshipStatus status);

    boolean existsByUser1IdAndUser2IdAndStatus(Long user1Id, Long user2Id, FriendshipStatus status);

@Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE " +
       "((f.user1.id = :user1Id AND f.user2.id = :user2Id) OR " +
       "(f.user1.id = :user2Id AND f.user2.id = :user1Id)) AND " +
       "f.status = 'ACCEPTED'")
boolean existsFriendshipBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
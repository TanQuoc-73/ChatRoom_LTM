package com.nhom8.chat.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom8.chat.entity.UserAvatar;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {
        Optional<UserAvatar> findFirstByUserIdAndCurrentTrue(Long userId);
        List<UserAvatar> findByUserIdOrderBySetAsAvatarAtDesc(Long userId);
        @Modifying
        @Query("UPDATE UserAvatar ua SET ua.current = false WHERE ua.user.id = :userId")
        void setAllAvatarsNotCurrent(@Param("userId") Long userId);
        
        @Query("SELECT ua FROM UserAvatar ua WHERE ua.user.id = :userId AND ua.current = true")
        Optional<UserAvatar> findByUserIdAndCurrentTrue(@Param("userId") Long userId);

        @Query("SELECT ua FROM UserAvatar ua WHERE ua.user.id = :userId ORDER BY ua.setAsAvatarAt DESC")
        Optional<UserAvatar> findLatestByUserId(@Param("userId") Long userId);
        
        boolean existsByUserIdAndCurrentTrue(Long userId);
}
package com.nhom8.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import com.nhom8.chat.entity.UserCoverPhoto;

public interface UserCoverPhotoRepository extends JpaRepository<UserCoverPhoto, Long> {
    Optional<UserCoverPhoto> findFirstByUserIdAndCurrentTrue(Long userId);
    List<UserCoverPhoto> findByUserIdOrderBySetAsCoverAtDesc(Long userId);
    
    @Modifying  
@Query("UPDATE UserCoverPhoto ucp SET ucp.current = false WHERE ucp.user.id = :userId")
void setAllCoverPhotosNotCurrent(@Param("userId") Long userId);
}
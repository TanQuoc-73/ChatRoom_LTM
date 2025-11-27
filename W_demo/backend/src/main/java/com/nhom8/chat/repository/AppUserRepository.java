package com.nhom8.chat.repository;

import com.nhom8.chat.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Search by username or displayName (case-insensitive, partial match)
    @Query("SELECT u FROM AppUser u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AppUser> searchByUsernameOrDisplayName(@Param("searchTerm") String searchTerm);
}

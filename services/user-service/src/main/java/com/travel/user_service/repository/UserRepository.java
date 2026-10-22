package com.travel.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.travel.user_service.models.*;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Optional<User> findByEmailIgnoreCase(String email);
    
    boolean existsByEmail(String email);

    boolean existsByFullName(String fullName);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) <> LOWER(:email)")
    Page<User> findAllExcludingEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE LOWER(u.email) <> LOWER(:email)")
    long countExcludingEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.state = true AND LOWER(u.email) <> LOWER(:email)")
    long countActiveExcludingEmail(@Param("email") String email);
}

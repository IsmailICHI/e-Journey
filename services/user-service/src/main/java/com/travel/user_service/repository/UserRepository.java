package com.travel.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.travel.user_service.models.*;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Optional<User> findByEmailIgnoreCase(String email);
    
    boolean existsByEmail(String email);

    boolean existsByFullName(String fullName);
}


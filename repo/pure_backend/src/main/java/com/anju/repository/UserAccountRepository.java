package com.anju.repository;

import com.anju.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
}

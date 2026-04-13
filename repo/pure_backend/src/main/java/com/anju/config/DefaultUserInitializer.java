package com.anju.config;

import com.anju.entity.UserAccount;
import com.anju.entity.UserRole;
import com.anju.repository.UserAccountRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DefaultUserInitializer {

    @Bean
    CommandLineRunner seedDefaultUsers(UserAccountRepository userAccountRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            if (userAccountRepository.findByUsername("admin").isEmpty()) {
                UserAccount admin = new UserAccount();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("Admin1234"));
                admin.setSecondaryPasswordHash(passwordEncoder.encode("SecAdmin1234"));
                admin.setEnabled(true);
                admin.setRoles(Set.of(UserRole.ADMIN, UserRole.FINANCE));
                userAccountRepository.save(admin);
            }
        };
    }
}

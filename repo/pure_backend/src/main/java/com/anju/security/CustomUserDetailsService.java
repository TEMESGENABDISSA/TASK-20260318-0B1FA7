package com.anju.security;

import com.anju.repository.UserAccountRepository;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toList()).toArray(String[]::new);
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(roles)
                .disabled(!user.isEnabled())
                .build();
    }
}

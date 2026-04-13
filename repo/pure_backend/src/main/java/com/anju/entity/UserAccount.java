package com.anju.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_account_username", columnNames = "username")
})
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "secondary_password_hash", nullable = false, length = 255)
    private String secondaryPasswordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = UserRole.class)
    @CollectionTable(name = "user_account_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 32)
    private Set<UserRole> roles = new HashSet<>();
}

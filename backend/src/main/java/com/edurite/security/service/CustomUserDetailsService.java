package com.edurite.security.service;

import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import java.util.LinkedHashSet;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named CustomUserDetailsService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CompanyProfileRepository companyProfileRepository;

    public CustomUserDetailsService(UserRepository userRepository, CompanyProfileRepository companyProfileRepository) {
        this.userRepository = userRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Override
    /**
     * this method handles the "loadUserByUsername" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .or(() -> userRepository.findByUsernameIgnoreCase(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = resolveEffectiveAuthorities(user).stream()
                .map(this::toAuthority)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return org.springframework.security.core.userdetails.User
                // JWT subjects are issued with the canonical email, so use the same principal value here.
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .authorities(authorities)
                .build();
    }

    private List<String> resolveEffectiveAuthorities(User user) {
        LinkedHashSet<String> authorities = user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean hasAdminAuthority = authorities.stream()
                .anyMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority));
        if (companyProfileRepository.findByUserId(user.getId()).isPresent() && !hasAdminAuthority) {
            authorities.remove("ROLE_STUDENT");
            authorities.add("ROLE_COMPANY");
        }
        return authorities.stream().toList();
    }

    /**
     * this method handles the "toAuthority" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String toAuthority(String roleName) {
        String normalized = roleName.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}


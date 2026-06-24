package com.edurite.security.service;

import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named CurrentUserService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * this method handles the "requireUser" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public User requireUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElseThrow(InvalidCredentialsException::new);
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    public User requireUserById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}


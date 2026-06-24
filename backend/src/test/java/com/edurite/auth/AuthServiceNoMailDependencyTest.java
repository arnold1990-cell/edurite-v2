package com.edurite.auth;

import com.edurite.auth.service.AuthService;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceNoMailDependencyTest {

    @Test
    void authServiceConstructorDoesNotDependOnMailBeans() {
        Constructor<?> constructor = Arrays.stream(AuthService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Class<?>[] parameterTypes = constructor.getParameterTypes();

        assertThat(parameterTypes)
                .extracting(Class::getName)
                .doesNotContain("org.springframework.mail.javamail.JavaMailSender")
                .doesNotContain("com.edurite.email.service.EmailService");
    }
}



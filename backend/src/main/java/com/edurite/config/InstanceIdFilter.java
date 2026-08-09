package com.edurite.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("!prod")
@ConditionalOnBean(InstanceIdentity.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InstanceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-EduRite-Instance";

    private final InstanceIdentity instanceIdentity;
    private final boolean responseHeaderEnabled;

    public InstanceIdFilter(
            InstanceIdentity instanceIdentity,
            @Value("${edurite.instance.response-header.enabled:false}") boolean responseHeaderEnabled
    ) {
        this.instanceIdentity = instanceIdentity;
        this.responseHeaderEnabled = responseHeaderEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (responseHeaderEnabled) {
            response.setHeader(HEADER_NAME, instanceIdentity.id());
        }
        filterChain.doFilter(request, response);
    }
}

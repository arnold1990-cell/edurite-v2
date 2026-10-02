package com.edurite.subscription.service;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class SubscriptionWebConfig implements WebMvcConfigurer {
    private final SubscriptionAccessInterceptor interceptor;
    public SubscriptionWebConfig(SubscriptionAccessInterceptor interceptor) { this.interceptor = interceptor; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor).addPathPatterns("/api/**"); }
}

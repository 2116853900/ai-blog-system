package com.aiblog.config;

import com.aiblog.security.ForumMutationGuardInterceptor;
import com.aiblog.security.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ForumMutationGuardInterceptor forumMutationGuardInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor,
                        ForumMutationGuardInterceptor forumMutationGuardInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.forumMutationGuardInterceptor = forumMutationGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
        registry.addInterceptor(forumMutationGuardInterceptor);
    }
}

package com.aiblog.config;

import com.aiblog.security.ForumMutationGuardInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ForumMutationGuardInterceptor forumMutationGuardInterceptor;

    public WebMvcConfig(ForumMutationGuardInterceptor forumMutationGuardInterceptor) {
        this.forumMutationGuardInterceptor = forumMutationGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(forumMutationGuardInterceptor);
    }
}

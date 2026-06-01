package com.aiblog.security;

import com.aiblog.entity.ForumUser;
import com.aiblog.service.ForumUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Component
public class ForumMutationGuardInterceptor implements HandlerInterceptor {

    private static final Set<String> FORUM_MUTATION_METHODS = Set.of("POST", "PUT", "DELETE");

    private final ForumUserService userService;
    private final ObjectMapper objectMapper;

    public ForumMutationGuardInterceptor(ForumUserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!requiresActiveForumUser(request)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return true;
        }

        return userService.findByUsername(auth.getName())
                .map(ForumUser::getId)
                .map(userService::isActiveForumUser)
                .orElse(true)
                ? true
                : block(response);
    }

    private boolean requiresActiveForumUser(HttpServletRequest request) {
        String method = request.getMethod();
        String path = requestPath(request);
        if (FORUM_MUTATION_METHODS.contains(method) && path.startsWith("/api/forum/")) {
            return true;
        }
        return "POST".equals(method) && ("/api/reports".equals(path) || "/api/comments".equals(path));
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private boolean block(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", "账号已被封禁，暂不能进行互动"));
        return false;
    }
}

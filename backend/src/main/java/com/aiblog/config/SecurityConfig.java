package com.aiblog.config;

import com.aiblog.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {}) // 使用 CorsConfig 提供的 CorsConfigurationSource
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/search", "/api/search/**",
                        "/api/related-resources", "/api/related-resources/**",
                        "/api/posts/**", "/api/skills/**",
                        "/api/mcps/**", "/api/api-stations/**", "/api/comments/**",
                        "/api/forum/categories/**", "/api/forum/threads/**",
                        "/api/resource-favorites/**", "/api/resource-reviews/**",
                        "/api/users/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/comments", "/api/submissions").permitAll()
                // 论坛写操作（需登录）
                .requestMatchers(HttpMethod.POST, "/api/forum/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/forum/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/forum/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
                // 后台接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}

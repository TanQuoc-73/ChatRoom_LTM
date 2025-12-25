package com.nhom8.chat.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nhom8.chat.security.SessionTokenFilter;

// cấu hình bảo mật tổng thể cho ứng dụng
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // cấu hình mã hóa mật khẩu
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // cấu hình spring security filter chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionTokenFilter filter) throws Exception {
        http
            // tắt csrf và sử dụng session stateless
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // cấu hình phân quyền truy cập
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/public/**",
                    "/public/html/**",
                    "/public/css/**",
                    "/public/js/**",
                    "/public/image/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/", "/index.html",
                    "/auth/**" 
                ).permitAll()
                .requestMatchers(
                    "/api/uploads/**",
                    "/uploads/**",
                    "/api/friends/**",
                    "/friends/**",
                    "/api/v1/auth/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/ws/**",
                    "/ws-native/**",
                    "/ws-chat/**",
                    "/api/v1/realtime/**"
                ).permitAll()

                    // Cho phép tất cả authenticated vào API admin
                    .requestMatchers("/admin/**").authenticated()

                    // Hoặc gộp chung
                    .requestMatchers("/api/**", "/admin/**").authenticated()
                // các request còn lại cần xác thực
                .anyRequest().authenticated()
            )
            // thêm filter xác thực token
            .addFilterBefore(
                filter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // cấu hình cors cho api
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

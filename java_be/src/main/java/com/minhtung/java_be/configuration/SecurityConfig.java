package com.minhtung.java_be.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*"); // Cho phép tất cả domain truy cập API
        corsConfiguration.addAllowedMethod("*"); // Cho phép tất cả phương thức HTTP (GET, POST, PUT, DELETE,...)
        corsConfiguration.addAllowedHeader("*"); // Cho phép tất cả các header

        // Đăng ký cấu hình CORS cho tất cả các API
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(urlBasedCorsConfigurationSource); // Trả về bộ lọc CORS
    }
}

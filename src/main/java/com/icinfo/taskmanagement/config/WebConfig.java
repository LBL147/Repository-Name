package com.icinfo.taskmanagement.config;

import com.icinfo.taskmanagement.security.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    public WebConfig(AuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/auth/**");
    }
}

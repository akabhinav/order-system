package com.oms.api.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public WebMvcConfig(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyHandlerInterceptor())
                .addPathPatterns("/api/**");
    }

    @Bean
    public IdempotencyHandlerInterceptor idempotencyHandlerInterceptor() {
        return new IdempotencyHandlerInterceptor(redisTemplate, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration() {
        FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MdcFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("mdcFilter");
        return registration;
    }
}

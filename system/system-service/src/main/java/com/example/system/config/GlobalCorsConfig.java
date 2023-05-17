package com.example.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {
    @Bean
    public CorsFilter getCorsFilter(){
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        //添加哪些方法可以跨域
        config.addAllowedMethod("*");
        //添加哪个来源可以跨域
        config.addAllowedOrigin("*");
        //设置哪些头信息可以跨域
        config.addAllowedHeader("*");
        //允许跨域发送cookie
        config.setAllowCredentials(true);
        //添加跨域策略
        configSource.registerCorsConfiguration("/**",config);
        return new CorsFilter(configSource);
    }
}

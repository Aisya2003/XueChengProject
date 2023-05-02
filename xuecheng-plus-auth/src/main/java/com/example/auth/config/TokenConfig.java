package com.example.auth.config;

import com.example.base.constant.SystemConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.Arrays;
import java.util.Collections;

import static com.example.base.constant.SystemConstant.SIGNING_KEY;

/**
 * @author Administrator
 * @version 1.0
 **/
@Configuration
public class TokenConfig {
    
    //使用Jwt令牌
    //设置令牌类型
    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    //设置令牌的签名
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setSigningKey(SystemConstant.SIGNING_KEY);
        return jwtAccessTokenConverter;
    }

    //令牌管理服务
    //避免重名
    @Bean(name = "authorizationServerTokenServiceCustom")
    public AuthorizationServerTokenServices authorizationServerTokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        //设置tokenStore
        defaultTokenServices.setTokenStore(tokenStore());
        //支持刷新令牌
        defaultTokenServices.setSupportRefreshToken(true);

        //令牌增强策略
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Collections.singletonList(jwtAccessTokenConverter()));
        defaultTokenServices.setTokenEnhancer(tokenEnhancerChain);

        //设置刷新令牌的有效时间
        defaultTokenServices.setRefreshTokenValiditySeconds(259200);
        //设置令牌的有效时间
        defaultTokenServices.setAccessTokenValiditySeconds(7200);

        return defaultTokenServices;
    }
}

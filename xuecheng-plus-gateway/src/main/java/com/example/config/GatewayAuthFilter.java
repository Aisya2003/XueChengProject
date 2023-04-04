package com.example.config;

import com.alibaba.fastjson.JSON;
import com.example.base.constant.SystemConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;


@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private final static List<String> whiteList;

    private final TokenStore tokenStore;

    @Autowired
    public GatewayAuthFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    //加载白名单
    static {
        try (InputStream resourceAsStream = GatewayAuthFilter.class.getResourceAsStream("/security-whiteList.properties");) {
            //读取文件信息
            Properties properties = new Properties();
            properties.load(resourceAsStream);
            Set<String> propertiesSet = properties.stringPropertyNames();
            whiteList = new ArrayList<>(propertiesSet);

        } catch (IOException e) {
            throw new RuntimeException("加载白名单出错！", e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求路径地址
        String requestUri = exchange.getRequest().getPath().value();
        AntPathMatcher antPathMatcher = new AntPathMatcher();


        //白名单放行
        for (String uri : whiteList) {
            if (antPathMatcher.match(uri, requestUri)) {
                return chain.filter(exchange);
            }
        }

        //非白名单需要校验token
        String token = getToken(exchange);
        if (StringUtils.isEmpty(token)) {
            return buildMono("没有认证信息！", exchange);
        }

        //开始校验
        try {
            OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(token);
            boolean expired = oAuth2AccessToken.isExpired();
            if (expired) {
                return buildMono("令牌已过期！", exchange);
            }
            return chain.filter(exchange);
        } catch (Exception e) {
            return buildMono("令牌认证无效！", exchange);
        }

    }

    /**
     * 构建返回Mono对象
     *
     * @param errMsg   信息
     * @param exchange 响应对象
     * @return httpResponse
     */
    private Mono<Void> buildMono(String errMsg, ServerWebExchange exchange) {
        //获取response
        ServerHttpResponse response = exchange.getResponse();
        //构建返回对象
        String errMsgJson = JSON.toJSONString(new RestErrorResponse(errMsg));
        byte[] bytes = errMsgJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        //设置response
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        //写回
        return response.writeWith(Mono.just(dataBuffer));
    }

    /**
     * 从request中获取token
     *
     * @param exchange 请求
     * @return tokenStr
     */
    private String getToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(SystemConstant.AUTH_HEADER);
        if (StringUtils.isEmpty(header)) {
            return null;
        }
        //获取token中的bearer后的信息
        String token = header.split(" ")[1];
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        return token;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

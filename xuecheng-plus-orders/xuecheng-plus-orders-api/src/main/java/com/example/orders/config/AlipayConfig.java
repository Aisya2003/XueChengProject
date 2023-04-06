package com.example.orders.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.example.orders.model.constant.AlipayConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AlipayConfig {
    @Value("${pay.alipay.APP_ID}")
    private String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    private String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY;

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                AlipayConstant.URL,
                APP_ID,
                APP_PRIVATE_KEY,
                AlipayConstant.FORMAT,
                AlipayConstant.CHARSET,
                ALIPAY_PUBLIC_KEY,
                AlipayConstant.SIGN_TYPE);
    }
}

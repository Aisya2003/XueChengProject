package com.example.orders.model.constant;

/**
 * @author Mr.M
 * @version 1.0
 * @description 支付宝配置参数
 * @date 2022/10/20 22:45
 */
public class AlipayConstant {
    public static String notify_url = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/notify_url.jsp";
    public static String return_url = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/return_url.jsp";
    public static String URL = "https://openapi.alipaydev.com/gateway.do";
    // 编码
    public static String CHARSET = "UTF-8";
    // 返回格式
    public static String FORMAT = "json";
    public static String log_path = "/log";
    public static String SIGN_TYPE = "RSA2";
    public static String PRODUCT_CODE = "QUICK_WAP_PAY";
    
}

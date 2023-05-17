package com.example.orders.model.dto;

import lombok.Data;


@Data
public class PayStatusDto {

    //商户订单号
    String out_trade_no;
    //支付宝交易号
    String trade_no;
    //交易状态
    String trade_status;
    //appid
    String app_id;
    //total_amount
    String total_amount;
}

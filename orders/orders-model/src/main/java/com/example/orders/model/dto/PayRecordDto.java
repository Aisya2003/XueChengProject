package com.example.orders.model.dto;

import com.example.orders.model.po.PayRecord;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class PayRecordDto extends PayRecord {

    //二维码
    private String qrcode;

}

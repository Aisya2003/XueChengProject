package com.example.orders.api;

import com.example.orders.model.dto.AddOrderDto;
import com.example.orders.model.dto.PayRecordDto;
import com.example.orders.service.IOrdersService;
import com.example.orders.util.GetUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Controller
@Slf4j
public class OrdersController {
    
    private final IOrdersService ordersService;

    @Autowired
    public OrdersController(IOrdersService ordersService) {
        this.ordersService = ordersService;
    }


    //生成二维码，保存订单和支付记录
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto dto) {
        String userId = Objects.requireNonNull(GetUser.getUser()).getId();
        return ordersService.generateCodeAndSaveOrderRecorder(userId, dto);

    }

    //支付
    @GetMapping("/requestpay")
    public void payOrders(@Param("payNo") String payNo, HttpServletResponse response) throws Exception {
        ordersService.requestAlipay(payNo, response);
    }

    //支付宝通知接口
    @RequestMapping("/notify")
    public void receiveResult(HttpServletRequest request, HttpServletResponse response) {
        ordersService.receiveResult(request, response);
    }
}

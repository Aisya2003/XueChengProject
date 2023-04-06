package com.example.orders.service;

import com.example.base.model.RestResponse;
import com.example.orders.model.dto.AddOrderDto;
import com.example.orders.model.dto.PayRecordDto;
import com.example.orders.model.dto.PayStatusDto;
import com.example.orders.model.po.Orders;
import com.example.orders.model.po.PayRecord;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IOrdersService {

    /**
     * 创建订单信息，返回支付二维码
     *
     * @param userId 创建订单者的id
     * @param dto    添加参数
     * @return 二维码和订单信息
     */
    PayRecordDto generateCodeAndSaveOrderRecorder(String userId, AddOrderDto dto);

    /**
     * 创建订单
     *
     * @param userId 用户id
     * @param dto    请求参数
     * @return 订单记录实体
     */
    Orders createOrders(String userId, AddOrderDto dto);

    /**
     * 生成订单并保存到订单表
     *
     * @param dto   请求参数
     * @param useId 用户id
     * @return 订单记录实体
     */
    Orders buildOrdersAndSave(AddOrderDto dto, String useId);

    /**
     * 根据订单信息保存商品详情
     *
     * @param saveOrders 订单详情
     */
    void saveOrdersDetail(Orders saveOrders);

    /**
     * 根据payNo查询支付表记录
     *
     * @param payNo 支付编号
     * @return 支付记录
     */
    PayRecord getPayRecordByPayNo(String payNo);

    void requestAlipay(String payNo, javax.servlet.http.HttpServletResponse response);

    /**
     * 生成支付记录
     *
     * @param orders 订单信息
     * @return 支付记录实体
     */
    PayRecord createPayRecord(Orders orders);

    /**
     * 主动获取订单支付结果
     *
     * @param payNo 支付记录号
     * @return 订单状态
     */
    PayStatusDto getPayResult(String payNo);

    /**
     * 接受支付宝返回的结果通知
     *
     * @param request  请求
     * @param response 响应
     */
    void receiveResult(HttpServletRequest request, HttpServletResponse response);
}

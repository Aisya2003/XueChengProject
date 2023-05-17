package com.example.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.constant.Dictionary;
import com.example.base.exception.BusinessException;
import com.example.base.utils.IPUtil;
import com.example.base.utils.IdWorkerUtils;
import com.example.base.utils.QRCodeUtil;
import com.example.messagesdk.service.MqMessageService;
import com.example.orders.config.OrderServiceRabbitMQConfig;
import com.example.orders.mapper.OrdersGoodsMapper;
import com.example.orders.mapper.OrdersMapper;
import com.example.orders.mapper.PayRecordMapper;
import com.example.orders.model.constant.AlipayConstant;
import com.example.orders.model.dto.AddOrderDto;
import com.example.orders.model.dto.PayRecordDto;
import com.example.orders.model.dto.PayStatusDto;
import com.example.orders.model.po.Orders;
import com.example.orders.model.po.OrdersGoods;
import com.example.orders.model.po.PayRecord;
import com.example.orders.service.IOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrdersServiceImpl implements IOrdersService {
    private final IOrdersService proxy;
    private final OrdersMapper ordersMapper;
    private final OrdersGoodsMapper ordersGoodsMapper;
    private final PayRecordMapper payRecordMapper;
    private final AlipayClient alipayClient;
    private final MqMessageService mqMessageService;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY;
    @Value("${pay.alipay.APP_ID}")
    private String APP_ID;
    @Value("{pay.alipay.notifyUrl}")
    private String notifyUrl;

    @Autowired
    @Lazy
    public OrdersServiceImpl(IOrdersService proxy, OrdersMapper ordersMapper, OrdersGoodsMapper ordersGoodsMapper, PayRecordMapper payRecordMapper, AlipayClient alipayClient, MqMessageService mqMessageService) {
        this.proxy = proxy;
        this.ordersMapper = ordersMapper;
        this.ordersGoodsMapper = ordersGoodsMapper;
        this.payRecordMapper = payRecordMapper;
        this.alipayClient = alipayClient;
        this.mqMessageService = mqMessageService;
    }

    @Override
    @Transactional
    public PayRecordDto generateCodeAndSaveOrderRecorder(String userId, AddOrderDto dto) {

        //创建商品订单
        Orders orders = proxy.createOrders(userId, dto);

        //添加支付信息
        PayRecord payRecord = proxy.createPayRecord(orders);

        //生成二维码
        String code = null;
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        try {
            code = qrCodeUtil.createQRCode("http://" + IPUtil.getLocalIP() + "/api/orders/requestpay?payNo=" +
                    payRecord.getPayNo(), 300, 300);
        } catch (IOException e) {
            BusinessException.cast("生成支付二维码失败");
        }

        //封装返回参数
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(code);

        return payRecordDto;
    }

    /**
     * 生成支付记录
     *
     * @param orders 订单信息
     * @return 支付记录实体
     */
    @Transactional
    public PayRecord createPayRecord(Orders orders) {
        //支付记录id不重复，使用雪花算法
        long payRecordNo = IdWorkerUtils.getInstance().nextId();
        PayRecord payRecord = new PayRecord();
        //支付记录
        payRecord.setPayNo(payRecordNo);
        //支付关联的订单id
        payRecord.setOrderId(orders.getId());
        //其他信息
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setUserId(orders.getUserId());
        payRecord.setStatus(Dictionary.PAYING_STATUS_UNPAID.getCode());

        //保存到数据库
        payRecordMapper.insert(payRecord);

        return payRecord;
    }

    @Override
    public PayStatusDto getPayResult(String payNo) {
        //请求
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        //构建请求参数
        Map<String, String> map = new HashMap<>();
        map.put("out_trade_no", payNo);
        request.setBizContent(JSON.toJSONString(map));

        //发起请求
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            BusinessException.cast("获取订单请求异常");
        }

        //调用成功
        if (response.isSuccess()) {
            //获取订单结果
            String body = response.getBody();
            HashMap<String, String> hashMap = JSON.parseObject(body, HashMap.class);

            //封装返回结果
            PayStatusDto payStatusDto = new PayStatusDto();
            payStatusDto.setApp_id(hashMap.get("app_id"));
            payStatusDto.setTrade_status(hashMap.get("trade_status"));
            payStatusDto.setTrade_no(hashMap.get("trade_no"));
            payStatusDto.setTotal_amount(hashMap.get("total_amount"));
            payStatusDto.setOut_trade_no(hashMap.get("out_trade_no"));


            return payStatusDto;
        }
        return null;
    }

    @Override
    public void receiveResult(HttpServletRequest request, HttpServletResponse response) {
        //获取支付宝POST过来反馈信息
        Map<String, String> params = new HashMap<>();
        Map requestParams = request.getParameterMap();
        for (Object o : requestParams.keySet()) {
            String name = (String) o;
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        //验签名
        boolean verify_result = false;
        try {
            verify_result = AlipaySignature.rsaCheckV1(
                    params,
                    ALIPAY_PUBLIC_KEY,
                    AlipayConstant.CHARSET,
                    AlipayConstant.SIGN_TYPE);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }

        if (verify_result) {
            //验证成功
            String outTradeNo = null;
            String tradeNo = null;
            String tradeStatus = null;
            String appId = null;
            String totalAmount = null;
            //商户订单号
            outTradeNo = this.getRequestParams(request, "out_trade_no");
            //支付宝交易号
            tradeNo = this.getRequestParams(request, "trade_no");
            //交易状态
            tradeStatus = this.getRequestParams(request, "trade_status");
            //支付宝通知的appid
            appId = this.getRequestParams(request, "app_id");
            //总金额
            totalAmount = this.getRequestParams(request, "total_amount");

            if (tradeStatus.equals("TRADE_SUCCESS")) {

                //封装更新参数
                PayStatusDto payStatusDto = new PayStatusDto();
                payStatusDto.setApp_id(appId);
                //支付结果
                payStatusDto.setTrade_status(tradeStatus);
                //第三方订单号
                payStatusDto.setOut_trade_no(outTradeNo);
                payStatusDto.setTotal_amount(totalAmount);
                //支付宝订单号
                payStatusDto.setTrade_no(tradeNo);

                //更新数据库
                proxy.updatePayStatus(payStatusDto, Dictionary.PAYING_METHOD_ALIPAY.getCode());

            }
            try {
                log.info("返回支付宝success信息");
                response.getWriter().println("success");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            //验证失败
            try {
                log.info("返回支付宝fail信息");
                response.getWriter().println("fail");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 更新订单状态和支付状态
     *
     * @param payStatusDto 支付宝返回的结果参数
     */
    @Transactional
    public void updatePayStatus(PayStatusDto payStatusDto, String payChannel) {
        //查询是否存在订单
        String payNo = payStatusDto.getOut_trade_no();
        LambdaQueryWrapper<PayRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayRecord::getPayNo, payNo);
        PayRecord payRecordFromDB = payRecordMapper.selectOne(queryWrapper);
        if (payRecordFromDB == null) BusinessException.cast("订单号不存在！");

        //订单存在则先校验信息是否一致
        if (!verifyInfo(payRecordFromDB, payStatusDto)) BusinessException.cast("支付信息异常！");

        //检查订单传入参数是否已完成
        if (Dictionary.ORDER_PAYING_STATUS_PAID.getCode().equals(payStatusDto.getTrade_status())) {
            log.info("请求更新的订单支付状态为已支付");
            return;
        }

        //更新支付记录
        PayRecord payRecordUpdate = new PayRecord();
        payRecordUpdate.setStatus(Dictionary.PAYING_STATUS_PAID.getCode());
        //对于支付记录数据记录，支付宝的订单为外支付编号
        payRecordUpdate.setOutPayNo(payStatusDto.getTrade_no());
        //第三方渠道
        payRecordUpdate.setOutPayChannel(payChannel);
        payRecordUpdate.setPaySuccessTime(LocalDateTime.now());

        int updatePayRecordResult = payRecordMapper.update(payRecordUpdate, queryWrapper);
        if (updatePayRecordResult <= 0) BusinessException.cast("更新支付记录出现异常");

        //更新订单状态
        Long orderId = payRecordFromDB.getOrderId();
        proxy.updateOrdersStatus(orderId);
    }

    /**
     * 校验提供的参数
     *
     * @param payRecordFromDB 支付记录
     * @param payStatusDto    请求参数
     * @return 比对结果
     */
    private boolean verifyInfo(PayRecord payRecordFromDB, PayStatusDto payStatusDto) {
        return payStatusDto.getApp_id().equals(APP_ID) &&
                Float.valueOf(payStatusDto.getTotal_amount()).equals(payRecordFromDB.getTotalPrice());
    }

    /**
     * 更新订单表信息
     *
     * @param orderId 订单号
     */
    @Transactional
    public void updateOrdersStatus(Long orderId) {
        Orders orders = ordersMapper.selectById(orderId);
        if (orders == null)
            BusinessException.cast("成功更新支付记录，但是记录所属的订单不存在，订单号" + orders);
        Orders ordersUpdate = new Orders();
        ordersUpdate.setStatus(Dictionary.ORDER_PAYING_STATUS_PAID.getCode());
        ordersUpdate.setId(orderId);
        ordersMapper.updateById(ordersUpdate);

        //插入消息表
        mqMessageService.addMessage(
                OrderServiceRabbitMQConfig.MESSAGE_TYPE,
                //选课id
                orders.getOutBusinessId(),
                //购买类型
                orders.getOrderType(),
                null
        );
        log.info("更新支付记录成功，更新订单信息成功");
    }

    /**
     * 从请求中获取参数的值
     *
     * @param request 请求
     * @param param   参数
     * @return 参数的值
     */
    private String getRequestParams(HttpServletRequest request, String param) {
        return new String(request.getParameter(param).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public Orders createOrders(String userId, AddOrderDto dto) {
        //判断是否存在相同的订单信息
        String outBusinessId = dto.getOutBusinessId();
        Orders orders = getOrders(outBusinessId);
        if (orders != null) {
            return orders;
        }

        //创建订单信息，保存到订单表中
        Orders saveOrders = proxy.buildOrdersAndSave(dto, userId);

        //保存订单信息到订单详情表中
        proxy.saveOrdersDetail(saveOrders);

        return saveOrders;
    }

    @Transactional
    public void saveOrdersDetail(Orders saveOrders) {
        String orderDetailJSON = saveOrders.getOrderDetail();
        Long orderId = saveOrders.getId();
        //一个订单可以包含多个商品
        List<OrdersGoods> ordersGoods = JSON.parseArray(orderDetailJSON, OrdersGoods.class);
        //保存到订单详情表
        ordersGoods.forEach(detail -> {
            //商品对应的订单
            detail.setOrderId(orderId);
            ordersGoodsMapper.insert(detail);

        });
    }

    @Override
    @Transactional
    public PayRecord getPayRecordByPayNo(String payNo) {
        LambdaQueryWrapper<PayRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayRecord::getPayNo, payNo);
        return payRecordMapper.selectOne(queryWrapper);
    }

    @Override
    public void requestAlipay(String payNo, HttpServletResponse response) {
        //查询支付记录表是否存在订单
        PayRecord payRecord = this.getPayRecordByPayNo(payNo);
        if (payRecord == null) BusinessException.cast("支付记录为空");

        //请求支付
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();

        //封装请求参数
        Map<String, String> map = new HashMap<>();
        map.put("out_trade_no", payRecord.getPayNo().toString());
        map.put("total_amount", payRecord.getTotalPrice().toString());
        map.put("subject", payRecord.getOrderName());
        map.put("product_code", AlipayConstant.PRODUCT_CODE);
        String requestParamsJSON = JSON.toJSONString(map);

        //设置请求参数
        request.setBizContent(requestParamsJSON);
        //通知地址
        request.setNotifyUrl(notifyUrl + "/api/orders/notify");

        //获取返回结果
        String resultForm = null;
        try {
            resultForm = alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            throw new RuntimeException("获取支付结果异常", e);
        }
        response.setContentType("text/html;charset=" + AlipayConstant.CHARSET);
        try {
            //响应
            response.getWriter().write(resultForm);
            response.getWriter().flush();
        } catch (IOException e) {
            throw new RuntimeException("返回响应结果异常", e);
        }
    }

    @Transactional
    public Orders buildOrdersAndSave(AddOrderDto dto, String userId) {
        Orders saveOrders = new Orders();
        //使用雪花算法保证在分布式系统中id唯一
        long id = IdWorkerUtils.getInstance().nextId();
        saveOrders.setId(id);
        BeanUtils.copyProperties(dto, saveOrders);
        saveOrders.setStatus(Dictionary.ORDER_PAYING_STATUS_UNPAID.getCode());
        saveOrders.setCreateDate(LocalDateTime.now());
        saveOrders.setUserId(userId);
        ordersMapper.insert(saveOrders);
        return saveOrders;
    }

    /**
     * 获取商品订单
     *
     * @param outBusinessId 选课记录表id
     * @return 商品订单实体
     */
    private Orders getOrders(String outBusinessId) {
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getOutBusinessId, outBusinessId);
        return ordersMapper.selectOne(queryWrapper);
    }
}

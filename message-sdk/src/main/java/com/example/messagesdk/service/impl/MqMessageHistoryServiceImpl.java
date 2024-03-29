package com.example.messagesdk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.messagesdk.mapper.MqMessageHistoryMapper;
import com.example.messagesdk.model.po.MqMessageHistory;
import com.example.messagesdk.service.MqMessageHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class MqMessageHistoryServiceImpl extends ServiceImpl<MqMessageHistoryMapper, MqMessageHistory> implements MqMessageHistoryService {

}

package com.example.messagesdk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.messagesdk.model.po.MqMessage;

import java.util.List;


public interface MqMessageService extends IService<MqMessage> {

    /**
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param count      扫描记录数
     * @return 消息记录
     */
    public List<MqMessage> getMessageList(int shardIndex, int shardTotal, String messageType, int count);

    /**
     * @param businessKey1 业务id
     * @param businessKey2 业务id
     * @param businessKey3 业务id
     * @return 消息内容
     */
    public MqMessage addMessage(String messageType, String businessKey1, String businessKey2, String businessKey3);

    /**
     * @param id 消息id
     * @return int 更新成功：1
     */
    public int completed(long id);

    public int completedStageOne(long id);

    public int completedStageTwo(long id);

    public int completedStageThree(long id);

    public int completedStageFour(long id);

    public int getStageOne(long id);

    public int getStageTwo(long id);

    public int getStageThree(long id);

    public int getStageFour(long id);

}

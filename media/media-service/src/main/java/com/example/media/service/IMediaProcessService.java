package com.example.media.service;

import com.example.media.model.po.MediaProcess;

import java.util.List;

public interface IMediaProcessService {
    /**
     * 根据分片参数和cpu的核心数，获取指定数量的待处理视频文件
     * @param shardTotal 分片总数
     * @param shardIndex 分片索引
     * @param coreCount 处理数量
     * @return 待处理视频文件
     */
    List<MediaProcess> getMediaProcessListByShardInfo(int shardTotal,int shardIndex,int coreCount);

    /**
     * 保存文件的处理结果
     * @param taskId 任务Id
     * @param status 处理后状态
     * @param fileId 文件Id
     * @param url 处理成功后的MinIoURL
     * @param errorMsg 处理失败的错误信息
     */
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);
}

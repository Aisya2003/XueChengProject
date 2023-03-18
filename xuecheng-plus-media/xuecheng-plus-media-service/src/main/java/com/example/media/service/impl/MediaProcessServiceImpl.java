package com.example.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.example.media.mapper.MediaFilesMapper;
import com.example.media.mapper.MediaProcessHistoryMapper;
import com.example.media.mapper.MediaProcessMapper;
import com.example.media.model.po.MediaFiles;
import com.example.media.model.po.MediaProcess;
import com.example.media.model.po.MediaProcessHistory;
import com.example.media.service.IMediaProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MediaProcessServiceImpl implements IMediaProcessService {
    private MediaProcessMapper mediaProcessMapper;
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    public MediaProcessServiceImpl(MediaProcessMapper mediaProcessMapper,MediaProcessHistoryMapper mediaProcessHistoryMapper,MediaFilesMapper mediaFilesMapper) {
        this.mediaProcessMapper = mediaProcessMapper;
        this.mediaFilesMapper = mediaFilesMapper;
        this.mediaProcessHistoryMapper = mediaProcessHistoryMapper;
    }

    @Override
    public List<MediaProcess> getMediaProcessListByShardInfo(int shardTotal, int shardIndex, int coreCount) {
        return mediaProcessMapper.selectListByShardInfo(shardTotal,shardIndex,coreCount);
    }

    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //查询这个任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null){
            log.debug("处理不存在的任务：{}",taskId);
            return;
        }

        //判断结果
        //创建更新规则
        LambdaQueryWrapper<MediaProcess> updateWapper = new LambdaQueryWrapper<>();
        updateWapper.eq(MediaProcess::getId,taskId);
        //"3"为失败
        MediaProcess mediaProcessResult = new MediaProcess();
        if ("3".equals(status)){
            //创建局部更新的对象
            mediaProcessResult.setStatus("3");
            mediaProcessResult.setErrormsg(errorMsg);
            mediaProcessResult.setFinishDate(LocalDateTime.now());
            //更新局部
            mediaProcessMapper.update(mediaProcessResult,updateWapper);
        }


        //成功则添加到History表中，并删除处理表中信息
        //"2"为成功
        if ("2".equals(status)){
            //创建局部更新的对象
            mediaProcessResult.setStatus("2");
            mediaProcessResult.setUrl(url);
            mediaProcessResult.setFinishDate(LocalDateTime.now());
            //更新局部
            mediaProcessMapper.update(mediaProcessResult,updateWapper);
            //添加到历史记录
            MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();

            //更新base表中的URL
            MediaFiles mediaFiles = new MediaFiles();
            mediaFiles.setUrl(url);
            LambdaQueryWrapper<MediaFiles> mediaFilesUpdateWrapper = new LambdaQueryWrapper<>();
            mediaFilesUpdateWrapper.eq(MediaFiles::getId,fileId);
            mediaFilesMapper.update(mediaFiles,mediaFilesUpdateWrapper);

            //查询更新后的mediaProcess
            mediaProcess = mediaProcessMapper.selectById(taskId);
            //拷贝属性
            BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
            mediaProcessHistoryMapper.insert(mediaProcessHistory);
            //删除处理表中数据
            mediaProcessMapper.deleteById(taskId);
        }


    }
}

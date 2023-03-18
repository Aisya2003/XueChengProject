package com.example.media.service.jobhandler;

import com.example.base.utils.Mp4VideoUtil;
import com.example.base.utils.StringUtil;
import com.example.media.mapper.MediaProcessMapper;
import com.example.media.model.po.MediaProcess;
import com.example.media.service.IMediaFileService;
import com.example.media.service.IMediaProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VideoTask {
    private MediaProcessMapper mediaProcessMapper;
    private IMediaFileService mediaFileService;
    private IMediaProcessService mediaProcessService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    @Autowired
    public VideoTask(MediaProcessMapper mediaProcessMapper, IMediaFileService mediaFileService, IMediaProcessService mediaProcessService) {
        this.mediaProcessMapper = mediaProcessMapper;
        this.mediaFileService = mediaFileService;
        this.mediaProcessService = mediaProcessService;
    }

    /**
     * 视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() {
        //获取分片信息
        int shardTotal = XxlJobHelper.getShardTotal();
        int shardIndex = XxlJobHelper.getShardIndex();
        //查询待处理任务
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardInfo(shardTotal, shardIndex, 2);
        if (mediaProcesses.isEmpty()) {
            log.debug("待处理视频的任务数为0");
            return;
        }
        //要处理的任务数
        int tasks = mediaProcesses.size();
        //启动多线程执行
        ExecutorService threadPool = Executors.newFixedThreadPool(tasks);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(tasks);
        mediaProcesses.forEach((mediaProcess) -> {
            threadPool.execute(() -> {
                //确认状态，保证幂等性
                if ("2".equals(mediaProcess.getStatus())) {
                    log.debug("视频文件已处理：{}", mediaProcess);
                    countDownLatch.countDown();
                    return;
                }
                //获取文件信息
                String bucket = mediaProcess.getBucket();
                String filename = mediaProcess.getFilename();
                String fileId = mediaProcess.getFileId();
                String filePath = mediaProcess.getFilePath();
                //将原始视频下载到本地
                //创建临时文件
                File sourceFile = null;
                File targetFile = null;
                try {
                    sourceFile = File.createTempFile("source", null);
                    targetFile = File.createTempFile("target", ".mp4");
                } catch (Exception e) {
                    log.error("创建临时文件失败");
                    countDownLatch.countDown();
                    return;
                }
                //下载
                try {
                    mediaFileService.getFilesByMinio(sourceFile, filePath, bucket);
                } catch (Exception e) {
                    log.error("下载源文件出错:{}，文件信息:{}", e.getMessage(), mediaProcess);
                    countDownLatch.countDown();
                    return;
                }
                //转换格式
                //转换后的文件名
                String targetFileName = StringUtil.isNotEmpty(filename) ? filename : fileId + ".mp4";
                //String ffmpeg_path, String video_path, String mp4_name, String mp4folder_path
                //ffmpeg执行路径，源文件地址，转换后的文件名称，转换后的文件位置
                Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, sourceFile.getAbsolutePath(), targetFileName, targetFile.getAbsolutePath());
                String result = videoUtil.generateMp4();
                //设置处理后的状态初始值
                String finalStatus = "3";
                //设置最终Url
                String finalUrl = null;
                if ("success".equals(result)) {
                    //转换成功，上传MinIO
                    String targetFileMinIoPath = getFilePathByMd5(fileId, ".mp4");
                    try {
                        mediaFileService.addMediaFileToMinio(targetFile.getAbsolutePath(), bucket, targetFileMinIoPath);
                        //记录状态
                        finalStatus = "2";
                        finalUrl = "/" + bucket + "/" + targetFileMinIoPath;
                    } catch (Exception e) {
                        log.error("上传到MinIO失败：{}", e.getMessage());
                        countDownLatch.countDown();
                        return;
                    }
                }
                //记录任务处理结果
                try {
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), finalStatus, fileId, finalUrl, result);
                } catch (Exception e) {
                    log.error("保存结果到数据库出错:{}", e.getMessage());
                    countDownLatch.countDown();
                    return;
                }
                //线程处理完成，计数器减一
                countDownLatch.countDown();
            });
        });
//        阻塞到所有任务结束
        try {
            countDownLatch.await(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error("计数器结束失败:{}",e.getMessage());
            throw new RuntimeException(e);
        }


    }

    /**
     * 根据文件md5获得对应的minio文件位置
     *
     * @param fileMd5   md5
     * @param extension 扩展名
     * @return 文件路径
     */
    private String getFilePathByMd5(String fileMd5, String extension) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + extension;
    }
}

package com.example.media.service;

import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.base.model.RestResponse;
import com.example.media.model.dto.QueryMediaParamsDto;
import com.example.media.model.dto.UploadFileParamsDto;
import com.example.media.model.dto.UploadMediaFilesDto;
import com.example.media.model.po.MediaFiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface IMediaFileService {
    /**
     * 上传大文件到minio
     *
     * @param path       文件的绝对路径
     * @param bucketName 桶
     * @param objectName 上传后的文件名
     */
    public void addMediaFileToMinio(String path, String bucketName, String objectName);

    /**
     * 从minio下载分块到本地
     *
     * @param chunkFile  本地文件信息
     * @param ObjectName 分块文件路径
     * @param bucketName 桶
     * @return 本地文件信息
     */
    public File getFilesByMinio(File chunkFile, String ObjectName, String bucketName);

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.example.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);


    /**
     * 上传文件的通用接口
     *
     * @param companyId  机构id
     * @param dto        上传文件的参数
     * @param bytes      file-data的字节数组
     * @param folder     指定的文件夹
     * @param objectName 指定的文件名
     * @return UploadMediaFilesDto
     */
    UploadMediaFilesDto uploadFiles(Long companyId, UploadFileParamsDto dto, byte[] bytes, String folder, String objectName);

    /**
     * 将媒资文件信息保存到数据库中
     *
     * @param companyId   机构id
     * @param dto         上传的参数
     * @param objectName  文件的桶内全路径
     * @param fileId      文件的id
     * @param bucketFiles 桶名称
     * @return 媒资表实体
     */
    @Transactional
    MediaFiles addMediaFilesToDb(Long companyId, UploadFileParamsDto dto, String objectName, String fileId, String bucketFiles);

    /**
     * 根据文件md5的值检查是否已经存在该文件了
     *
     * @param fileMd5 文件的md5值
     * @return
     */
    RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * 根据文件的md5的值和chunk编号，来确定那些块已经上传
     *
     * @param fileMd5 文件的md5
     * @param chunk   分块编号
     * @return
     */
    RestResponse<Boolean> checkChunk(String fileMd5, int chunk);

    /**
     * 上传分块
     *
     * @param file    文件流
     * @param fileMd5 文件md5
     * @param chunk   分块序号
     * @return 处理结果
     */
    RestResponse uploadChunk(MultipartFile file, String fileMd5, int chunk);

    /**
     * 合并分块
     *
     * @param fileMd5    文件的md5
     * @param fileName   文件名
     * @param chunkTotal 分块总数
     * @return 处理结果
     */
    RestResponse mergeChunks(Long companyId, String fileMd5, String fileName, int chunkTotal, UploadFileParamsDto dto);

    /**
     * 获取文件的Url地址
     *
     * @param mediaId 文件id
     * @return Url
     */
    RestResponse<String> getFileUrlById(String mediaId);
}

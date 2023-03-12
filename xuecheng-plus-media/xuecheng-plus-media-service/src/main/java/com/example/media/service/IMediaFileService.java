package com.example.media.service;

import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.media.model.dto.QueryMediaParamsDto;
import com.example.media.model.dto.UploadFileParamsDto;
import com.example.media.model.dto.UploadMediaFilesDto;
import com.example.media.model.po.MediaFiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface IMediaFileService {

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
}

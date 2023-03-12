package com.example.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.base.exception.XuechengPlusException;
import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.media.mapper.MediaFilesMapper;
import com.example.media.model.dto.UploadFileParamsDto;
import com.example.media.model.dto.UploadMediaFilesDto;
import com.example.media.service.IMediaFileService;
import com.example.media.model.dto.QueryMediaParamsDto;
import com.example.media.model.po.MediaFiles;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
public class MediaFileServiceImpl implements IMediaFileService {
    private MediaFilesMapper mediaFilesMapper;
    private MinioClient minioClient;
    private IMediaFileService currentProxy;

    //获取普通文件桶的信息
    @Value("${minio.bucket.files}")
    private String bucketFiles;


    @Autowired
    @Lazy
    public MediaFileServiceImpl(MediaFilesMapper mediaFilesMapper, MinioClient minioClient, IMediaFileService mediaFileService) {
        this.mediaFilesMapper = mediaFilesMapper;
        this.minioClient = minioClient;
        this.currentProxy = mediaFileService;
    }

    @Override
    public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadMediaFilesDto uploadFiles(Long companyId, UploadFileParamsDto dto, byte[] bytes, String folder, String objectName) {
        //判断用户是否自己设置了目录
        if (StringUtils.isEmpty(folder)) {
            folder = getFileFolder(new Date(), true, true, true);

        } else if (!folder.contains("/")) {
            //如果用户提供的文件夹不包含“/”，则进行处理
            folder = folder + "/";
        }
        //上传时默认的文件名称
        String filename = dto.getFilename();
        //获取文件md5
        String fileMd5 = DigestUtils.md5DigestAsHex(bytes);
        ///判断用户是否自己设置了文件名
        if (StringUtils.isEmpty(objectName)) {
            //未指定则默认使用文件的md5值作为objectName
            objectName = fileMd5;
            //添加文件扩展名
            objectName = objectName + filename.substring(filename.lastIndexOf("."));
        }
        //拼接文件夹和文件
        objectName = folder + objectName;
        MediaFiles mediaFiles = null;
        //通知Spring事务出错
        try {
            //上传minio
            this.addMediaFileToMinio(objectName, bytes, bucketFiles);
            //写入数据库
            //通过注入被Spring代理的自身，实现事务控制
            mediaFiles = currentProxy.addMediaFilesToDb(companyId, dto, objectName, fileMd5, bucketFiles);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        //返回数据
        UploadMediaFilesDto uploadMediaFilesDto = new UploadMediaFilesDto();
        BeanUtils.copyProperties(mediaFiles, uploadMediaFilesDto);
        return uploadMediaFilesDto;
    }

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
    public MediaFiles addMediaFilesToDb(Long companyId, UploadFileParamsDto dto, String objectName, String fileId, String bucketFiles) {
        String filename = dto.getFilename();
        //检查数据库文件是否存在
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            //封装数据
            BeanUtils.copyProperties(dto, mediaFiles);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setFileId(fileId);
            mediaFiles.setId(fileId);
            mediaFiles.setFilename(filename);
            mediaFiles.setBucket(bucketFiles);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setUrl("/" + bucketFiles + "/" + objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setStatus("1");
            mediaFiles.setAuditStatus("002003");
            //插入
            try {
                mediaFilesMapper.insert(mediaFiles);
            } catch (Exception e) {
                XuechengPlusException.cast("上传数据库失败");
            }
        }
        return mediaFiles;
    }

    /**
     * 将日期转换为文件夹的格式
     *
     * @param date  日期
     * @param year  是否生成年级文件夹
     * @param month 是否生成月级文件夹
     * @param day   是否生成日级文件夹
     * @return 以“/”分割的日期字符串
     */
    private String getFileFolder(Date date, boolean year, boolean month, boolean day) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = simpleDateFormat.format(date);
        String[] splitDate = dateString.split("-");
        StringBuffer buffer = new StringBuffer();
        if (year) {
            buffer.append(splitDate[0]);
            buffer.append("/");
        }
        if (month) {
            buffer.append(splitDate[1]);
            buffer.append("/");
        }
        if (day) {
            buffer.append(splitDate[2]);
            buffer.append("/");
        }
        return buffer.toString();
    }

    /**
     * 将媒资文件添加到Minio中
     *
     * @param objectName  文件全程，包括文件夹+文件名
     * @param bytes       上传的文件
     * @param bucketFiles 上传到哪一个桶内
     */
    private void addMediaFileToMinio(String objectName, byte[] bytes, String bucketFiles) {
        try (
                //生成byteArray输入流
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ) {
            //获取contentType（默认设置为未知的二进制流）
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            //获取ObjectName的后缀名
            if (objectName.contains(".")) {
                ContentInfo extension = ContentInfoUtil.findExtensionMatch(objectName.substring(objectName.lastIndexOf(".")));
                //匹配到了对应的ContentType
                if (extension != null) {
                    contentType = extension.getMimeType();
                }
            }

            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketFiles)
                    .object(objectName)
                    //InputStream stream, long objectSize, long partSize
                    //输入流，对象大小，分片大小（-1表示5M，最大不超过5T，最多10000）
                    .stream(byteArrayInputStream, byteArrayInputStream.available(), -1)
                    .contentType(contentType)
                    .build();
            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            XuechengPlusException.cast("上传Minio失败！");
        }
    }
}

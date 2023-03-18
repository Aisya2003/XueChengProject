package com.example.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.base.exception.XuechengPlusException;
import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.base.model.RestResponse;
import com.example.media.mapper.MediaFilesMapper;
import com.example.media.mapper.MediaProcessMapper;
import com.example.media.model.dto.UploadFileParamsDto;
import com.example.media.model.dto.UploadMediaFilesDto;
import com.example.media.model.po.MediaProcess;
import com.example.media.service.IMediaFileService;
import com.example.media.model.dto.QueryMediaParamsDto;
import com.example.media.model.po.MediaFiles;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Upload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

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
    @Value("${minio.bucket.videofiles}")
    private String bucketVideoFiles;

    private MediaProcessMapper mediaProcessMapper;


    @Autowired
    @Lazy
    public MediaFileServiceImpl(MediaFilesMapper mediaFilesMapper, MinioClient minioClient, IMediaFileService mediaFileService, MediaProcessMapper mediaProcessMapper) {
        this.mediaFilesMapper = mediaFilesMapper;
        this.minioClient = minioClient;
        this.currentProxy = mediaFileService;
        this.mediaProcessMapper = mediaProcessMapper;
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

            //对应非image和mp4类型的文件Url值暂时不存储在数据库中，需要任务调度处理
            String mimeType = null;
            if (filename.contains(".")) {
                String extension = filename.substring(filename.indexOf("."));
                mimeType = this.getMimeTypeByExtension(extension);
            }
            if (mimeType.contains("image") || mimeType.contains("mp4")) {
                mediaFiles.setUrl("/" + bucketFiles + "/" + objectName);
            }
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setStatus("1");
            mediaFiles.setAuditStatus("002003");
            //插入
            try {
                mediaFilesMapper.insert(mediaFiles);
            } catch (Exception e) {
                XuechengPlusException.cast("上传数据库失败");
            }
            //对avi格式的文件进行处理
            if (mimeType.contains("x-msvideo")) {
                MediaProcess mediaProcess = new MediaProcess();
                BeanUtils.copyProperties(mediaFiles, mediaProcess);
                //设置状态未处理
                mediaProcess.setStatus("1");
                mediaProcessMapper.insert(mediaProcess);
            }
        }
        return mediaFiles;
    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //检查文件是否记录在数据库中
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            //不存在，开始上传
            return RestResponse.success(true);
        }
        //文件是否存在于minio中
        //桶
        String bucket = mediaFiles.getBucket();
        String objectName = mediaFiles.getFilePath();
        //抛异常也会返回false，不存在null
        Boolean success = findFileInMinio(bucket, objectName);
        //文件已存在
        if (success) {
            return RestResponse.success(success);
        } else {
            return RestResponse.validfail(false, "文件已存在！");
        }
    }

    /**
     * 检查文件是否存在于minio中
     *
     * @param bucket     桶名称
     * @param objectName 文件路径
     * @return 是否存在
     */
    private Boolean findFileInMinio(String bucket, String objectName) {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build();

        try (InputStream inputStream = minioClient.getObject(getObjectArgs)) {
            if (inputStream == null) {
                //不存在
                return false;
            }
        } catch (Exception e) {
            //文件不存在
            return false;
        }
        //存在
        return true;
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunk) {
        //获取文件的分块目录
        String chunkFolder = getFolderByMd5(fileMd5);

        //分块文件的路径
        String chunkFile = chunkFolder + chunk;

        //查询文件是否存在
        Boolean success = findFileInMinio(bucketVideoFiles, chunkFile);

        //success为false时继续上传
        return RestResponse.success(success);
    }

    @Override
    public RestResponse uploadChunk(MultipartFile file, String fileMd5, int chunk) {
        String folderByMd5 = this.getFolderByMd5(fileMd5);
        String filePath = folderByMd5 + chunk;
        try {
            byte[] bytes = file.getBytes();
            this.addMediaFileToMinio(filePath, bytes, bucketVideoFiles);
            return RestResponse.success(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public RestResponse mergeChunks(Long companyId, String fileMd5, String fileName, int chunkTotal, UploadFileParamsDto dto) {
        //分块文件
        File[] files = null;
        //临时合并文件
        File tempMergeFile = null;
        //下载分块

        try {
            files = this.checkAndDownLoadChunkStatus(fileMd5, chunkTotal);
            //升序排序文件
            List<File> listFiles = Arrays.asList(files);
            //获取文件扩展名
            String extension = fileName.substring(fileName.lastIndexOf("."));
            //目标目录
            try {
                tempMergeFile = File.createTempFile("merge", extension);
            } catch (IOException e) {
                XuechengPlusException.cast("创建合并临时文件失败");
            }

            try (
                    //创建写入文件流
                    RandomAccessFile randomAccessFileWrite = new RandomAccessFile(tempMergeFile, "rw");) {
                for (File listFile : listFiles) {
                    int len = -1;
                    //读取分块文件数据
                    RandomAccessFile randomAccessFileRead = new RandomAccessFile(listFile, "r");
                    //创建缓冲
                    byte[] bytes = new byte[1024 * 5];
                    try {
                        while ((len = randomAccessFileRead.read(bytes)) != -1) {
                            //写入数据
                            randomAccessFileWrite.write(bytes, 0, len);
                        }
                    } finally {
                        try {
                            randomAccessFileRead.close();
                        } catch (IOException e) {
                            XuechengPlusException.cast("关闭文件流失败！");
                        }
                    }
                }

            } catch (Exception e) {
                XuechengPlusException.cast("合并文件过程失败！");
            }
            //校验文件是否正确
            try {
                FileInputStream fileInputStream = new FileInputStream(tempMergeFile);
                String mergeMd5 = DigestUtils.md5DigestAsHex(fileInputStream);
                if (!mergeMd5.equals(fileMd5)) {
                    XuechengPlusException.cast("文件校验失败！");
                }
            } catch (IOException e) {
                XuechengPlusException.cast("文件校验出错！");
            }


            //将合并后的文件上传到minio
            String objectName = this.getFilePathByMd5(fileMd5, extension);
            //Minio的重载upLoad方法
            this.addMediaFileToMinio(tempMergeFile.getAbsolutePath(), bucketVideoFiles, objectName);
            //上传到数据库
            //设置文件大小
            dto.setFileSize(tempMergeFile.length());
            currentProxy.addMediaFilesToDb(companyId, dto, objectName, fileMd5, bucketVideoFiles);

            return RestResponse.success(true);
        } finally {
            //删除临时分块文件
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            if (tempMergeFile != null) {
                tempMergeFile.delete();
            }
        }
    }

    @Override
    public RestResponse<String> getFileUrlById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        if (mediaFiles == null) {
            XuechengPlusException.cast("文件不存在");
        }
        String url = mediaFiles.getUrl();
        if (StringUtils.isEmpty(url)) {
            XuechengPlusException.cast("文件正在处理中，请稍后！");
        }
        return RestResponse.success(url);
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

    /**
     * 下载分块
     *
     * @param fileMd5    文件的md5
     * @param chunkTotal 总分块数
     * @return 文件数组
     */

    private File[] checkAndDownLoadChunkStatus(String fileMd5, int chunkTotal) {
        //获取目录
        String folderByMd5 = getFolderByMd5(fileMd5);
        //创建文件数组
        File[] chunkFiles = new File[chunkTotal];
        File chunkFile = null;
        //开始下载
        for (int i = 0; i < chunkTotal; i++) {
            //每一个分块的路径
            String chunkFilePath = folderByMd5 + i;
            //创建临时目录
            try {
                chunkFile = File.createTempFile("chunk", null);
            } catch (IOException e) {
                XuechengPlusException.cast("创建临时分块文件失败！");
            }
            //获取每一个分块,然后下载
            chunkFile = getFilesByMinio(chunkFile, chunkFilePath, bucketVideoFiles);
            chunkFiles[i] = chunkFile;

        }
        return chunkFiles;

    }


    public File getFilesByMinio(File chunkFile, String ObjectName, String bucketName) {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(ObjectName)
                .build();
        try (
                //获取输入流
                InputStream inputStream = minioClient.getObject(getObjectArgs);
                //根据输出文件路径获取文件输入流
                FileOutputStream fileOutputStream = new FileOutputStream(chunkFile);) {
            //拷贝输入流到输出流
            IOUtils.copy(inputStream, fileOutputStream);
            //将输出流写入文件
            return chunkFile;
        } catch (Exception e) {
            XuechengPlusException.cast("获取分块异常，请重新上传文件！");
        }
        return null;
    }

    /**
     * 根据文件的md5值获取目录
     *
     * @param fileMd5 文件md5
     * @return 文件夹分块路径
     */
    private String getFolderByMd5(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/chunk/";
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
    
    @Override
    public void addMediaFileToMinio(String path, String bucketName, String objectName) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .filename(path)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
        } catch (Exception e) {
            XuechengPlusException.cast("上传大文件到minio出错！");
        }
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
            if (objectName.contains(".")) {
                //获取ObjectName的后缀名
                String extension = objectName.substring(objectName.lastIndexOf("."));
                contentType = getMimeTypeByExtension(extension);
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

    /**
     * 根据文件后缀名获取文件的MimeType
     *
     * @param extension 后缀名
     * @return MimeType
     */
    private String getMimeTypeByExtension(String extension) {
        //设置默认类型
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (!StringUtils.isEmpty(extension)) {
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
            if (extensionMatch != null) {
                contentType = extensionMatch.getMimeType();
            }
        }
        return contentType;
    }
}

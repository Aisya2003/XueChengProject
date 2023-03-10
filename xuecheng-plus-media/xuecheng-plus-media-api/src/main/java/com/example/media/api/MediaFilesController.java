package com.example.media.api;

import com.example.base.exception.XuechengPlusException;
import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.media.model.dto.UploadFileParamsDto;
import com.example.media.model.dto.UploadMediaFilesDto;
import com.example.media.model.dto.QueryMediaParamsDto;
import com.example.media.model.po.MediaFiles;
import com.example.media.service.IMediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {


    @Autowired
    IMediaFileService mediaFileService;


    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 22L;
        return mediaFileService.queryMediaFiles(companyId, pageParams, queryMediaParamsDto);

    }

    /**
     * 定义上传文件的类型为multipart/form-data
     * 使用MediaType指定上传类型
     *
     * @return
     */
    @ApiOperation("媒资上传接口")
    @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadMediaFilesDto upLoad(
            @RequestPart("filedata") MultipartFile fileData,
            @RequestParam(value = "folder",required = false) String folder,
            @RequestParam(value = "objectName",required = false)String objectName) {
        /**
         * 上传文件的字节流，提高接口通用性
         */
        //封装参数
        Long companyId = 22L;
        UploadFileParamsDto dto = new UploadFileParamsDto();
        dto.setContentType(fileData.getContentType());
        dto.setFileSize(fileData.getSize());
        dto.setFilename(fileData.getOriginalFilename());
        //设置文件类型
        if (fileData.getContentType().contains("image")){
            //图片
            dto.setFileType("001001");
        }else{
            //非图片
            dto.setFileType("001003");
        }
        UploadMediaFilesDto uploadMediaFilesDto = null;
        try {
            uploadMediaFilesDto = mediaFileService.uploadFiles(companyId, dto, fileData.getBytes(), folder, objectName);
        } catch (IOException e) {
            throw new XuechengPlusException("上传过程失败！");
        }
        return uploadMediaFilesDto;
    }

}

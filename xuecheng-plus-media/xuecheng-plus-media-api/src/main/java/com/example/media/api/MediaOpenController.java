package com.example.media.api;

import com.example.base.exception.XuechengPlusException;
import com.example.base.model.RestResponse;
import com.example.media.service.IMediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open")
public class MediaOpenController {
    private IMediaFileService mediaFileService;

    @Autowired
    public MediaOpenController(IMediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    @ApiOperation("预览文件")
    @GetMapping("/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable("mediaId")String mediaId){
        RestResponse<String> fileUrlById = mediaFileService.getFileUrlById(mediaId);
        String url = fileUrlById.getResult();
        if (StringUtils.isEmpty(url)){
            XuechengPlusException.cast("视频正在处理中！");
            return null;
        }
        return RestResponse.success(url);
    }
}

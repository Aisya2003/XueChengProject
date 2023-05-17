package com.example.media.api;

import com.example.base.model.RestResponse;
import com.example.media.model.dto.UploadFileParamsDto;
import com.example.media.service.IMediaFileService;
import com.example.media.util.GetUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
public class BigFileController {
    private final IMediaFileService mediaFileService;

    @Autowired
    public BigFileController(IMediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkFile(
            @RequestParam("fileMd5") String fileMd5
    ) throws Exception {
        return mediaFileService.checkFile(fileMd5);
    }


    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkChunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk) throws Exception {
        return mediaFileService.checkChunk(fileMd5, chunk);

    }

    @PostMapping("/upload/uploadchunk")
    public RestResponse uploadChunk(@RequestParam("file") MultipartFile file,
                                    @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("chunk") int chunk) throws Exception {
        return mediaFileService.uploadChunk(file, fileMd5, chunk);


    }

    @PostMapping("/upload/mergechunks")
    public RestResponse mergeChunks(@RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("fileName") String fileName,
                                    @RequestParam("chunkTotal") int chunkTotal) throws Exception {

        Long companyId = Long.valueOf(Objects.requireNonNull(GetUser.getUser()).getCompanyId());
        UploadFileParamsDto dto = new UploadFileParamsDto();
        dto.setFileType("001002");
        dto.setFilename(fileName);
        dto.setTags("课程视频");
        return mediaFileService.mergeChunks(companyId, fileMd5, fileName, chunkTotal, dto);

    }

}

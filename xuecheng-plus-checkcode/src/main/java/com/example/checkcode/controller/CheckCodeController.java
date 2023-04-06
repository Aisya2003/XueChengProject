package com.example.checkcode.controller;

import com.example.checkcode.model.dto.CheckCodeParamsDto;
import com.example.checkcode.model.dto.CheckCodeResultDto;
import com.example.checkcode.service.ICheckCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CheckCodeController {

    private final ICheckCodeService checkCodeService;

    @Autowired
    public CheckCodeController(ICheckCodeService checkCodeService) {
        this.checkCodeService = checkCodeService;
    }

    //生成验证码图片
    @PostMapping(value = "/pic")
    public CheckCodeResultDto generatePicCheckCode(CheckCodeParamsDto checkCodeParamsDto) {
        return checkCodeService.generate(checkCodeParamsDto);
    }

    //验证
    @PostMapping(value = "/verify")
    public Boolean verify(@RequestParam("key") String key, @RequestParam("code") String code) {
        return checkCodeService.verify(key, code);
    }

    //发送手机验证码
    @PostMapping(value = "/phone")
    public void sendPhoneCode(@RequestParam("param1") String phoneNumber) {
        checkCodeService.sendPhoneCode(phoneNumber);
    }
}

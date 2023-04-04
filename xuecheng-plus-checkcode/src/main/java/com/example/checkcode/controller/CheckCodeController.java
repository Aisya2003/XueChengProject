package com.example.checkcode.controller;

import com.example.checkcode.model.dto.CheckCodeParamsDto;
import com.example.checkcode.model.dto.CheckCodeResultDto;
import com.example.checkcode.service.ICheckCodeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
public class CheckCodeController {

    @Resource
    private ICheckCodeService checkCodeService;


    @PostMapping(value = "/pic")
    public CheckCodeResultDto generatePicCheckCode(CheckCodeParamsDto checkCodeParamsDto) {
        return checkCodeService.generate(checkCodeParamsDto);
    }

    @PostMapping(value = "/verify")
    public Boolean verify(@RequestParam("key") String key, @RequestParam("code") String code) {
        return checkCodeService.verify(key, code);
    }

    @PostMapping(value = "/phone")
    public void sendPhoneCode(@RequestParam("param1") String phoneNumber) {
        checkCodeService.sendPhoneCode(phoneNumber);
    }
}

package com.example.checkcode.service;

import com.example.checkcode.model.dto.CheckCodeParamsDto;
import com.example.checkcode.model.dto.CheckCodeResultDto;
import com.example.checkcode.model.dto.MailRequest;

public interface ICheckCodeService {
    /**
     * 生成验证码信息
     *
     * @param checkCodeParamsDto 请求参数
     * @return 结果参数
     */
    public CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto);

    /**
     * 验证验证码
     *
     * @param key  key
     * @param code checkCode
     * @return 比对结果
     */
    public Boolean verify(String key, String code);

    /**
     * 生成手机验证码
     *
     * @param phoneNumber 手机号
     */
    public void sendPhoneCode(String phoneNumber);

    /**
     * 生成邮箱验证码
     *
     * @param emailTo     目标邮箱
     * @param mailRequest 发送请求参数
     */
    void sendEmailCode(String emailTo, MailRequest mailRequest);
}

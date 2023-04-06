package com.example.ucenter.service;

import com.example.ucenter.model.dto.AuthParamsDto;
import com.example.ucenter.model.dto.UserExt;

public interface IAuthService {
    /**
     * 认证
     *
     * @param dto 请求参数
     * @return 用户对象的扩展
     */
    public UserExt auth(AuthParamsDto dto);


}

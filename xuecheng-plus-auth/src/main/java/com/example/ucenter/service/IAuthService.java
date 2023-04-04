package com.example.ucenter.service;

import com.example.ucenter.model.dto.AuthParamsDto;
import com.example.ucenter.model.dto.XcUserExt;
import com.example.ucenter.model.po.XcUser;

public interface IAuthService {
    /**
     * 认证
     *
     * @param dto 请求参数
     * @return 用户对象的扩展
     */
    public XcUserExt auth(AuthParamsDto dto);


}

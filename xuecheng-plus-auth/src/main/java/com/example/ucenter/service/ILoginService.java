package com.example.ucenter.service;

import com.example.base.model.RestResponse;
import com.example.ucenter.model.dto.FindPasswordDto;
import com.example.ucenter.model.dto.RegisterParamsDto;
import com.example.ucenter.model.po.User;
import com.example.ucenter.model.po.UserRole;

public interface ILoginService {
    /**
     * 注册
     *
     * @param dto 参数
     * @return 结果
     */
    RestResponse<Boolean> register(RegisterParamsDto dto);

    /**
     * 获取用户实体
     *
     * @param dto 参数
     * @return 用户实体
     */
    User buildUser(RegisterParamsDto dto);

    /**
     * 获取权限用户实体
     *
     * @param user 用户实体
     * @return 用户权限实体
     */
    UserRole buildUserRole(User user);

    /**
     * 通过手机号找回密码
     *
     * @param dto
     * @return true | false
     */
    RestResponse<Boolean> findPassword(FindPasswordDto dto);
}

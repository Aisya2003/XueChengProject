package com.example.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.base.exception.BusinessException;
import com.example.ucenter.mapper.MenuMapper;
import com.example.ucenter.model.dto.AuthParamsDto;
import com.example.ucenter.model.dto.UserExt;
import com.example.ucenter.model.po.Menu;
import com.example.ucenter.service.IAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户认证
 */
@Service("myUserService")
@Slf4j
public class UserServiceImpl implements UserDetailsService {

    //获取Spring容器
    private final ApplicationContext applicationContext;
    private final MenuMapper menuMapper;

    @Autowired
    public UserServiceImpl(ApplicationContext applicationContext, MenuMapper menuMapper) {
        this.applicationContext = applicationContext;
        this.menuMapper = menuMapper;
    }


    /**
     * @param s AuthParamsDto的JSONString
     * @return 用户完整信息
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            log.error("转换认证参数失败！认证参数：{}", e);
            throw new RuntimeException(e.getMessage());
        }


        String authType = authParamsDto.getAuthType();
        //获取Bean
        IAuthService authService = applicationContext.getBean(authType + "Auth", IAuthService.class);
        //认证
        UserExt userExt = authService.auth(authParamsDto);

        //构建认证信息
        return buildUserDetails(userExt);

    }

    /**
     * 构建UserDetail对象
     * 将用户的相关信息以JSON串的形式封装到username
     *
     * @param userExt 用户信息
     * @return userDetail
     */
    private UserDetails buildUserDetails(UserExt userExt) {

        //获取用户权限数组
        String[] authorities = buildUserAuthorities(userExt.getId());

        //隐藏用户密码
        userExt.setPassword("*");
        String userInfoJson = JSON.toJSONString(userExt);
        //封装返回对象
        return User.withUsername(userInfoJson)
                .authorities(authorities)
                .password("*")
                .build();
    }

    /**
     * 获取用户权限
     *
     * @param userId 用户id
     * @return 权限信息数组
     */
    private String[] buildUserAuthorities(String userId) {
        List<Menu> userAuthoritiesList = menuMapper.getAuthoritiesByUserId(userId);
        //获取用户权限
        List<String> itemList = new ArrayList<>();
        userAuthoritiesList.forEach(menu -> {
            //获取可执行的行为代码
            String item = menu.getCode();
            //保存
            itemList.add(item);
        });
        //封装权限
        String[] authorities = null;
        if (!itemList.isEmpty()) {
            authorities = itemList.toArray(new String[0]);

        }
        //登录的用户没有任何权限
        if (authorities == null) {
            BusinessException.cast("非法用户！");
        }
        return authorities;
    }

}

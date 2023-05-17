package com.example.orders.util;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Slf4j
public class GetUser {

    public static XcUser getUser() {
        //获取用户认证信息
        Object userInfo = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userInfo == null) {
            log.error("用户未认证！");
            return null;
        }
        if (userInfo instanceof String) {
            try {
                return JSON.parseObject(userInfo.toString(), XcUser.class);

            } catch (Exception e) {
                log.error("解析令牌身份出错！userJson：{}", userInfo);
            }
        }
        return null;
    }


    //作为工具类的静态内部类
    @Data
    public static class XcUser implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;

        private String username;

        private String password;

        private String salt;

        private String name;
        private String nickname;
        private String wxUnionid;
        private String companyId;
        /**
         * 头像
         */
        private String userpic;

        private String utype;

        private LocalDateTime birthday;

        private String sex;

        private String email;

        private String cellphone;

        private String qq;

        /**
         * 用户状态
         */
        private String status;

        private LocalDateTime createTime;

        private LocalDateTime updateTime;


    }

}

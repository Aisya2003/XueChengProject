package com.example.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.utils.StringUtil;
import com.example.ucenter.feignclient.CheckCodeClient;
import com.example.ucenter.mapper.XcUserMapper;
import com.example.ucenter.model.dto.AuthParamsDto;
import com.example.ucenter.model.dto.XcUserExt;
import com.example.ucenter.model.po.XcUser;
import com.example.ucenter.service.IAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service(value = "passwordAuth")
@Slf4j
public class PasswordAuthService implements IAuthService {
    private final XcUserMapper xcUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final CheckCodeClient checkCodeClient;

    @Autowired
    public PasswordAuthService(XcUserMapper xcUserMapper, PasswordEncoder passwordEncoder, CheckCodeClient checkCodeClient) {
        this.xcUserMapper = xcUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.checkCodeClient = checkCodeClient;
    }

    @Override
    public XcUserExt auth(AuthParamsDto dto) {
        //校验验证码
//        checkCode(dto);


        String username = dto.getUsername();
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (xcUser == null) {
            throw new UsernameNotFoundException("用户不存在!");
        }
        //获取用户成功
        String passwordFromDto = dto.getPassword();
        String passwordFromDB = xcUser.getPassword();

        //开始验证
        boolean matches = passwordEncoder.matches(passwordFromDto, passwordFromDB);
        if (!matches) {
            throw new RuntimeException("用户名或密码错误！");
        }

        //封装返回对象
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);

        return xcUserExt;
    }

    /**
     * 校验验证码
     *
     * @param dto 参数
     */
    public void checkCode(AuthParamsDto dto) {
        //校验验证码
        String checkCode = dto.getCheckcode();
        String key = dto.getCheckcodekey();
        if (StringUtil.isEmpty(checkCode) || StringUtil.isEmpty(key)) {
            throw new RuntimeException("验证码参数为空");
        }

        Boolean verify = checkCodeClient.verify(key, checkCode);
        if (verify == null || !verify) {
            throw new RuntimeException("验证码错误");
        }
    }
}

package com.example.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.exception.XuechengPlusException;
import com.example.base.model.RestResponse;
import com.example.base.utils.StringUtil;
import com.example.ucenter.mapper.XcUserMapper;
import com.example.ucenter.model.dto.AuthParamsDto;
import com.example.ucenter.model.dto.FindPasswordDto;
import com.example.ucenter.model.dto.RegisterParamsDto;
import com.example.ucenter.model.po.XcUser;
import com.example.ucenter.model.po.XcUserRole;
import com.example.ucenter.service.ILoginService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LoginServiceImpl implements ILoginService {
    private final XcUserMapper xcUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordAuthService passwordAuthService;
    private final GitHubAuthService gitHubAuthService;


    @Autowired
    public LoginServiceImpl(XcUserMapper xcUserMapper, PasswordEncoder passwordEncoder, PasswordAuthService passwordAuthService, GitHubAuthService gitHubAuthService) {
        this.xcUserMapper = xcUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordAuthService = passwordAuthService;
        this.gitHubAuthService = gitHubAuthService;
    }

    @Override
    public RestResponse<Boolean> register(RegisterParamsDto dto) {
        String username = dto.getUsername();
        XcUser userFromDB = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(!StringUtil.isEmpty(username), XcUser::getUsername, username));
        if (userFromDB != null) {
            XuechengPlusException.cast("用户名已存在！");
        }


        XcUser user = buildUser(dto);
        XcUserRole userRole = buildUserRole(user);
        Boolean result = gitHubAuthService.addUserAndUserRole(user, userRole);
        return result ? RestResponse.success() : null;
    }

    @Override
    public XcUser buildUser(RegisterParamsDto dto) {
        //校验验证码
        AuthParamsDto checkCodeDto = new AuthParamsDto();
        checkCodeDto.setCheckcode(dto.getCheckcode());
        checkCodeDto.setCheckcodekey("login:" + dto.getCellphone());
        passwordAuthService.checkCode(checkCodeDto);


        XcUser user = new XcUser();
        String username = dto.getUsername();
        String originPassword = dto.getPassword();
        String confirmPwd = dto.getConfirmpwd();

        //校验密码
        String password = checkAndEncodePassword(originPassword, confirmPwd);
        String email = dto.getEmail();

        String cellPhone = dto.getCellphone();
        String nickName = StringUtil.isEmpty(dto.getNickname()) ? username : dto.getNickname();
        String id = UUID.randomUUID().toString();

        user.setUsername(username);
        user.setPassword(password);
        user.setId(id);
        user.setNickname(nickName);
        user.setUtype("101001");
        user.setStatus("1");
        user.setCreateTime(LocalDateTime.now());
        user.setCellphone(cellPhone);
        user.setEmail(email);
        user.setName(username);


        return user;
    }

    /**
     * 校验密码并加密
     *
     * @param originPassword 原始密码
     * @param confirmPwd     确认密码
     * @return 加密后的密码
     */
    private String checkAndEncodePassword(String originPassword, String confirmPwd) {
        if (StringUtil.isEmpty(originPassword) ||
                StringUtil.isEmpty(confirmPwd) ||
                !originPassword.equals(confirmPwd)
        ) {
            throw new RuntimeException("密码输入错误！");
        }
        return passwordEncoder.encode(originPassword);
    }

    @Override
    public XcUserRole buildUserRole(XcUser user) {
        XcUserRole xcUserRole = new XcUserRole();
        String userRoleId = UUID.randomUUID().toString();
        xcUserRole.setId(userRoleId);
        xcUserRole.setUserId(user.getId());
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRole.setRoleId("17");
        return xcUserRole;
    }

    @Override
    public RestResponse<Boolean> findPassword(FindPasswordDto dto) {


        String cellphone = dto.getCellphone();
        if (!StringUtil.isEmpty(cellphone)) {
            return findPasswordByPhone(dto, cellphone);

        }
        String email = dto.getEmail();
        if (StringUtil.isEmpty(email)) {
            XuechengPlusException.cast("手机号和邮箱地址不能全为空！");
        }

        return null;


    }

    /**
     * 通过手机号码找回密码
     *
     * @param dto       请求参数
     * @param cellphone 手机号
     * @return 找回结果
     */
    private RestResponse<Boolean> findPasswordByPhone(FindPasswordDto dto, String cellphone) {
        //校验验证码
        AuthParamsDto authParamsDto = new AuthParamsDto();
        authParamsDto.setCheckcodekey("login:" + cellphone);
        authParamsDto.setCheckcode(dto.getCheckcode());
        passwordAuthService.checkCode(authParamsDto);

        //校验用户
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getCellphone, cellphone));
        if (user == null) {
            XuechengPlusException.cast("此手机号未绑定账号，请先注册！");
        }
        String originPassword = dto.getPassword();
        String confirmpwd = dto.getConfirmpwd();
        String password = checkAndEncodePassword(originPassword, confirmpwd);

        //更新用户
        LambdaQueryWrapper<XcUser> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(XcUser::getCellphone, cellphone);
        XcUser updateUser = new XcUser();
        updateUser.setPassword(password);

        int result = xcUserMapper.update(updateUser, updateWrapper);

        return result > 0 ? RestResponse.success() : null;
    }
}

package com.example.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.exception.BusinessException;
import com.example.base.model.RestResponse;
import com.example.ucenter.mapper.UserMapper;
import com.example.ucenter.model.dto.AuthParamsDto;
import com.example.ucenter.model.dto.FindPasswordDto;
import com.example.ucenter.model.dto.RegisterParamsDto;
import com.example.ucenter.model.po.User;
import com.example.ucenter.model.po.UserRole;
import com.example.ucenter.service.ILoginService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LoginServiceImpl implements ILoginService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordAuthService passwordAuthService;
    private final GitHubAuthService gitHubAuthService;


    @Autowired
    public LoginServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, PasswordAuthService passwordAuthService, GitHubAuthService gitHubAuthService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordAuthService = passwordAuthService;
        this.gitHubAuthService = gitHubAuthService;
    }

    @Override
    public RestResponse<Boolean> register(RegisterParamsDto dto) {
        String username = dto.getUsername();
        User userFromDB = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(!StringUtils.isEmpty(username), User::getUsername, username));
        if (userFromDB != null) {
            BusinessException.cast("用户名已存在！");
        }


        User user = buildUser(dto);
        UserRole userRole = buildUserRole(user);
        Boolean result = gitHubAuthService.addUserAndUserRole(user, userRole);
        return result ? RestResponse.success() : null;
    }

    @Override
    public User buildUser(RegisterParamsDto dto) {
        //校验验证码
        AuthParamsDto checkCodeDto = new AuthParamsDto();
        checkCodeDto.setCheckcode(dto.getCheckcode());
        checkCodeDto.setCheckcodekey("login:" + dto.getCellphone());
        passwordAuthService.checkCode(checkCodeDto);


        User user = new User();
        String username = dto.getUsername();
        String originPassword = dto.getPassword();
        String confirmPwd = dto.getConfirmpwd();

        //校验密码
        String password = checkAndEncodePassword(originPassword, confirmPwd);
        String email = dto.getEmail();

        String cellPhone = dto.getCellphone();
        String nickName = StringUtils.isEmpty(dto.getNickname()) ? username : dto.getNickname();
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
        if (StringUtils.isEmpty(originPassword) ||
                StringUtils.isEmpty(confirmPwd) ||
                !originPassword.equals(confirmPwd)
        ) {
            throw new RuntimeException("密码错误！");
        }
        return passwordEncoder.encode(originPassword);
    }

    @Override
    public UserRole buildUserRole(User user) {
        UserRole userRole = new UserRole();
        String userRoleId = UUID.randomUUID().toString();
        userRole.setId(userRoleId);
        userRole.setUserId(user.getId());
        userRole.setCreateTime(LocalDateTime.now());
        userRole.setRoleId("17");
        return userRole;
    }

    @Override
    public RestResponse<Boolean> findPassword(FindPasswordDto dto) {
        //优先使用手机号找回密码
        String cellphone = dto.getCellphone();
        if (!StringUtils.isEmpty(cellphone)) {
            return findPasswordByPhone(dto, cellphone);

        }


        String email = dto.getEmail();
        if (StringUtils.isEmpty(email)) {
            BusinessException.cast("手机号和邮箱地址不能全为空！");
        }


        return findPasswordByEmail(dto, dto.getEmail());


    }

    /**
     * 邮箱找回密码
     *
     * @param dto   请求参数
     * @param email 邮箱地址
     * @return 找回结果
     */
    private RestResponse<Boolean> findPasswordByEmail(FindPasswordDto dto, String email) {
        checkCodeValidation(dto, email);
        //校验用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            BusinessException.cast("此邮箱未绑定账号，请先注册！");
        }

        int result = updatePasswordAfterFindPassword(dto, email);

        return result > 0 ? RestResponse.success() : null;
    }

    /**
     * 通过手机号码找回密码
     *
     * @param dto       请求参数
     * @param cellphone 手机号
     * @return 找回结果
     */
    private RestResponse<Boolean> findPasswordByPhone(FindPasswordDto dto, String cellphone) {
        checkCodeValidation(dto, cellphone);

        //校验用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getCellphone, cellphone));
        if (user == null) {
            BusinessException.cast("此手机号未绑定账号，请先注册！");
        }


        int result = updatePasswordAfterFindPassword(dto, cellphone);

        return result > 0 ? RestResponse.success() : null;
    }

    /**
     * 找回密码后更新密码
     *
     * @param dto 请求参数
     * @param key 更新关键字
     * @return 更新结果
     */
    private int updatePasswordAfterFindPassword(FindPasswordDto dto, String key) {
        String originPassword = dto.getPassword();
        String confirmpwd = dto.getConfirmpwd();
        String password = checkAndEncodePassword(originPassword, confirmpwd);

        //更新用户
        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getCellphone, key).or().eq(User::getEmail, key);
        User updateUser = new User();
        updateUser.setPassword(password);

        return userMapper.update(updateUser, updateWrapper);
    }

    /**
     * 检查验证码
     *
     * @param dto 请求参数
     * @param key 校验参数
     */
    private void checkCodeValidation(FindPasswordDto dto, String key) {
        //校验验证码
        AuthParamsDto authParamsDto = new AuthParamsDto();
        authParamsDto.setCheckcodekey("login:" + key);
        authParamsDto.setCheckcode(dto.getCheckcode());
        passwordAuthService.checkCode(authParamsDto);
    }
}

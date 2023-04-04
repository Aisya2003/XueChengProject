package com.example.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.auth.util.RestTemplateUtil;
import com.example.base.utils.StringUtil;
import com.example.ucenter.mapper.XcUserMapper;
import com.example.ucenter.mapper.XcUserRoleMapper;
import com.example.ucenter.model.dto.AuthParamsDto;
import com.example.ucenter.model.dto.TokenDto;
import com.example.ucenter.model.dto.XcUserExt;
import com.example.ucenter.model.po.XcUser;
import com.example.ucenter.model.po.XcUserRole;
import com.example.ucenter.service.IAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service(value = "githubAuth")
public class GitHubAuthService implements IAuthService {
    private final RestTemplateUtil restTemplateUtil;
    private final PasswordEncoder passwordEncoder;
    private final XcUserMapper xcUserMapper;
    private final XcUserRoleMapper xcUserRoleMapper;
    private final GitHubAuthService proxy;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String client_id;
    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String client_secret;

    @Autowired
    @Lazy
    public GitHubAuthService(RestTemplateUtil restTemplateUtil, PasswordEncoder passwordEncoder, XcUserMapper xcUserMapper, XcUserRoleMapper xcUserRoleMapper, GitHubAuthService proxy, StringRedisTemplate stringRedisTemplate) {
        this.restTemplateUtil = restTemplateUtil;
        this.passwordEncoder = passwordEncoder;
        this.xcUserMapper = xcUserMapper;
        this.xcUserRoleMapper = xcUserRoleMapper;
        this.proxy = proxy;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public XcUserExt auth(AuthParamsDto dto) {
        //开始验证
        String username = dto.getUsername();
        XcUser user = xcUserMapper.selectOne(
                new LambdaQueryWrapper<XcUser>().eq(!StringUtil.isEmpty(username),
                        XcUser::getUsername,
                        username));
        if (user == null) {
            throw new RuntimeException("用户不存在！");
        }

        //防止非法登录
        String githubUnionid = user.getGithubUnionid();
        String result = stringRedisTemplate.opsForValue().get("login:github:" + githubUnionid);
        if (StringUtil.isEmpty(result)) throw new RuntimeException("用户登录出错！请重新登录");


        user.setPassword("user-password");
        XcUserExt userExt = new XcUserExt();
        BeanUtils.copyProperties(user, userExt);
        //登录结束
        stringRedisTemplate.delete("login:github:" + githubUnionid);
        return userExt;
    }
    

    /**
     * 获取GitHub用户信息
     *
     * @param code 临时code
     * @return 用户实体
     */
    public XcUser getUser(String code) {
        //通用的Header
        HttpHeaders headers = new HttpHeaders();
        //获取token，设置header的content-type
        TokenDto accessToken = getTokenDto(code, headers);
        //从资源服务器获取用户信息
        XcUser userInfo = getUserInfo(headers, accessToken);

        //登录信息存储至redis临时保存
        stringRedisTemplate.opsForValue().setIfAbsent(
                "login:github:" + userInfo.getGithubUnionid(),
                "verified", Duration.ofMinutes(5));

        return userInfo;
    }

    /**
     * 携带令牌去资源服务器获取用户信息，并封装
     *
     * @param headers     头信息
     * @param accessToken token令牌
     * @return 用户实体
     */
    private XcUser getUserInfo(HttpHeaders headers, TokenDto accessToken) {
        //携带token的信息
        headers.set("Authorization", "token " + accessToken.getAccess_token());
        String getUserInfoUrl = "https://api.github.com/user";
        ResponseEntity<String> response = restTemplateUtil.get(getUserInfoUrl, headers, String.class, Collections.emptyMap());
        //登录的用户信息
        String userInfoJson = response.getBody();
        HashMap<String, Object> userMap = null;
        try {
            userMap = new ObjectMapper().readValue(userInfoJson, HashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("用户信息转换失败！");
        }

        //获取用户信息
        String userName = userMap.get("login").toString();
        String githubId = userMap.get("id").toString();


        //查询用户
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(!StringUtil.isEmpty(githubId), XcUser::getGithubUnionid, githubId);
        XcUser userFromDB = xcUserMapper.selectOne(queryWrapper);
        if (userFromDB != null) return userFromDB;

        //第一次授权，添加用户信息到数据库
        XcUser xcUser = null;
        XcUserRole xcUserRole = null;
        //封装用户信息
        String userId = UUID.randomUUID().toString();
        xcUser = buildUser(userId, userName, githubId);
        //封装用户权限
        xcUserRole = buildUserRole(userId);

        //添加信息到数据库
        Boolean result = proxy.addUserAndUserRole(xcUser, xcUserRole);

        return result ? xcUser : null;
    }

    /**
     * 保存信息到数据库
     *
     * @param xcUser     用户实体
     * @param xcUserRole 用户权限实体
     * @return 插入结果
     */
    @Transactional
    public Boolean addUserAndUserRole(XcUser xcUser, XcUserRole xcUserRole) {
        int insertUser = xcUserMapper.insert(xcUser);
        int insertRole = xcUserRoleMapper.insert(xcUserRole);
        return insertUser > 0 && insertRole > 0;
    }

    /**
     * 添加用户到权限表
     *
     * @param userId 用户id
     * @return 用户权限实体
     */
    private XcUserRole buildUserRole(String userId) {
        XcUserRole xcUserRole = new XcUserRole();
        String userRoleId = UUID.randomUUID().toString();
        xcUserRole.setId(userRoleId);
        xcUserRole.setUserId(userId);
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRole.setRoleId("17");
        return xcUserRole;
    }

    /**
     * 构建user实体
     *
     * @param userId   用户id
     * @param userName 用户名
     * @param githubId GitHub唯一表示
     * @return 用户实体
     */
    private XcUser buildUser(String userId, String userName, String githubId) {
        XcUser xcUser = new XcUser();
        xcUser.setUsername(userName);
        xcUser.setCreateTime(LocalDateTime.now());
        xcUser.setStatus("1");
        xcUser.setId(userId);
        xcUser.setGithubUnionid(githubId);
        xcUser.setUtype("101001");
        xcUser.setPassword(passwordEncoder.encode(userId));
        xcUser.setName(userName);
        xcUser.setNickname(userName);
        return xcUser;
    }

    /**
     * 获取token
     *
     * @param code    临时code
     * @param headers 全局头信息，添加Content-Type：application/JSON
     * @return token
     */
    private TokenDto getTokenDto(String code, HttpHeaders headers) {
        //获取令牌的url
        String getAccessTokenUrl = "https://github.com/login/oauth/access_token";

        //设置请求头
        List<MediaType> mediaTypes = new ArrayList<>();
        //请求头accept设置为：application/json
        mediaTypes.add(MediaType.APPLICATION_JSON);
        headers.setAccept(mediaTypes);

        //设置请求体
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("client_id", client_id);
        requestBody.put("client_secret", client_secret);
        requestBody.put("code", code);


        //发出请求
        //请求路径，请求头，请求体，请求响应结果类型，路径参数
        ResponseEntity<String> responseEntity = restTemplateUtil.post(getAccessTokenUrl, headers, requestBody, String.class, Collections.emptyMap());

        //获取GitHub返回的令牌
        String accessTokenJson = responseEntity.getBody();
        //转换为TokenDto
        TokenDto accessToken = null;
        try {
            accessToken = new ObjectMapper().readValue(accessTokenJson, TokenDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("转换token令牌出错");
        }
        return accessToken;
    }
}

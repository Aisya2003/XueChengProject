package com.example.auth.controller;

import com.example.ucenter.model.po.User;
import com.example.ucenter.service.impl.GitHubAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GitHubLoginController {

    private final GitHubAuthService gitHubAuthService;

    @Autowired
    public GitHubLoginController(GitHubAuthService gitHubAuthService) {
        this.gitHubAuthService = gitHubAuthService;
    }


    /**
     * 从GitHub上获取令牌，访问资源服务器获取资源
     *
     * @param code grant_type为code时GitHub返回的临时code
     */
    @RequestMapping("/githublogin")
    public String githubLogin(String code, String state) throws Exception {
        User user = gitHubAuthService.getUser(code);

        if (user != null) {
            return "redirect:http://www.51xuecheng.cn/sign.html?username=" + user.getUsername()
                    + "&authType=github";
        } else {
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
    }


}

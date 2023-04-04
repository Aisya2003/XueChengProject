package com.example.auth.controller;

import com.example.ucenter.model.dto.FindPasswordDto;
import com.example.ucenter.model.dto.RegisterParamsDto;
import com.example.base.model.RestResponse;
import com.example.ucenter.service.ILoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {
    private final ILoginService loginService;

    @Autowired
    public LoginController(ILoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/register")
    public RestResponse<Boolean> register(@RequestBody RegisterParamsDto dto) {
        return loginService.register(dto);
    }

    @PostMapping("/findpassword")
    public RestResponse<Boolean> findPassword(@RequestBody FindPasswordDto dto) {
        return loginService.findPassword(dto);
    }
}

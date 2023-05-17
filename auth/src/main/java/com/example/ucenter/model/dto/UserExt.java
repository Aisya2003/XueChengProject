package com.example.ucenter.model.dto;

import com.example.ucenter.model.po.User;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class UserExt extends User {
    //用户权限
    List<String> permissions = new ArrayList<>();
}

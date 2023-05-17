package com.example.ucenter.model.dto;

import lombok.Data;

@Data
public class FindPasswordDto {
    private String cellphone;
    private String checkcode;
    private String checkcodekey;
    private String confirmpwd;
    private String email;
    private String password;

}

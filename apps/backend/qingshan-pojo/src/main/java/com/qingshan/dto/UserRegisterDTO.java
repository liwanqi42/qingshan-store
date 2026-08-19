package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户注册
 */
@Data
public class UserRegisterDTO implements Serializable {

    private String phone;
    private String password;
    private String name;

}

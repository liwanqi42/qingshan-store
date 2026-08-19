package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户登录
 */
@Data
public class UserLoginDTO implements Serializable {

    // wx.login 返回的登录凭证
    private String code;

    // 手机号密码登录
    private String phone;
    private String password;

    // 新版手机号快速验证组件返回的 code
    private String phoneCode;

    // 旧版手机号授权返回的加密数据
    private String encryptedData;

    // 旧版手机号授权返回的加密向量
    private String iv;

}

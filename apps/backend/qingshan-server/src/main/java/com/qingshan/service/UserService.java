package com.qingshan.service;

import com.qingshan.dto.UserLoginDTO;
import com.qingshan.dto.UserRegisterDTO;
import com.qingshan.entity.User;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);

    /**
     * 手机号密码登录
     * @param phone 手机号
     * @param password 密码
     * @return
     */
    User loginByPassword(String phone, String password);

    /**
     * 手机号注册
     * @param userRegisterDTO 注册信息
     * @return
     */
    User register(UserRegisterDTO userRegisterDTO);
}

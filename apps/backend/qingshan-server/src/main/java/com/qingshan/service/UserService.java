package com.qingshan.service;

import com.qingshan.dto.UserLoginDTO;
import com.qingshan.entity.User;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}

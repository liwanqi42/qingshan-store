package com.qingshan.service;

import com.qingshan.dto.UserLoginDTO;
import com.qingshan.dto.UserProfileDTO;
import com.qingshan.dto.UserRegisterDTO;
import com.qingshan.entity.User;
import com.qingshan.vo.UserProfileVO;

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

    /**
     * 查询当前用户资料
     * @param userId 用户id
     * @return 用户资料
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新当前用户资料
     * @param userId 用户id
     * @param userProfileDTO 昵称、头像、性别
     * @return 更新后的用户资料
     */
    UserProfileVO updateProfile(Long userId, UserProfileDTO userProfileDTO);
}

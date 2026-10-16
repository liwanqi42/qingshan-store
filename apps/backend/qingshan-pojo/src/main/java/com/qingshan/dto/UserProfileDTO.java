package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户资料更新
 */
@Data
public class UserProfileDTO implements Serializable {

    //昵称
    private String name;

    //头像地址
    private String avatar;

    //性别 0 女 1 男
    private String sex;

}

package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户个人资料
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //昵称
    private String name;

    //头像
    private String avatar;

    //性别 0 女 1 男
    private String sex;

    //手机号（当前用户本人可见）
    private String phone;

}

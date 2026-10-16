package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginVO implements Serializable {

    private Long id;
    private String phone;
    private String token;
    //昵称
    private String name;
    //头像
    private String avatar;
    //性别 0 女 1 男
    private String sex;

}

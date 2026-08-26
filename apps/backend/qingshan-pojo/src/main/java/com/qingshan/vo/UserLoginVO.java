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
    // TODO(security): 前端未实际使用，收尾时移除，避免把微信身份标识下发给客户端
    private String openid;
    private String phone;
    private String token;

}

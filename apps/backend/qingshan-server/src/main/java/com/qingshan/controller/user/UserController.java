package com.qingshan.controller.user;

import com.qingshan.constant.JwtClaimsConstant;
import com.qingshan.dto.UserLoginDTO;
import com.qingshan.dto.UserRegisterDTO;
import com.qingshan.entity.User;
import com.qingshan.properties.JwtProperties;
import com.qingshan.result.Result;
import com.qingshan.service.UserService;
import com.qingshan.utils.JwtUtil;
import com.qingshan.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/user/user")
@Api(tags = "C端用户相关接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录：手机号密码登录或微信登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        User user;
        if (StringUtils.hasText(userLoginDTO.getPhone()) && StringUtils.hasText(userLoginDTO.getPassword())) {
            log.info("手机号密码登录:{}", userLoginDTO.getPhone());
            user = userService.loginByPassword(userLoginDTO.getPhone(), userLoginDTO.getPassword());
        } else {
            log.info("微信登录:{}", userLoginDTO.getCode());
            user = userService.wxLogin(userLoginDTO);
        }
        return Result.success(buildLoginVO(user));
    }

    /**
     * 手机号注册
     * @param userRegisterDTO
     * @return
     */
    @PostMapping("/register")
    @ApiOperation("手机号注册")
    public Result<UserLoginVO> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        log.info("手机号注册:{}", userRegisterDTO.getPhone());
        User user = userService.register(userRegisterDTO);
        return Result.success(buildLoginVO(user));
    }

    /**
     * 生成登录返回结果
     * @param user
     * @return
     */
    private UserLoginVO buildLoginVO(User user) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        return UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .phone(user.getPhone())
                .token(token)
                .build();
    }
}

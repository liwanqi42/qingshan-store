package com.qingshan.controller.user;

import com.qingshan.constant.JwtClaimsConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.UserLoginDTO;
import com.qingshan.dto.UserProfileDTO;
import com.qingshan.dto.UserRegisterDTO;
import com.qingshan.entity.User;
import com.qingshan.properties.JwtProperties;
import com.qingshan.result.Result;
import com.qingshan.service.UserService;
import com.qingshan.service.support.LoginRateLimiter;
import com.qingshan.service.support.TokenBlacklistService;
import com.qingshan.utils.JwtUtil;
import com.qingshan.vo.UserLoginVO;
import com.qingshan.vo.UserProfileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
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
    @Autowired
    private LoginRateLimiter loginRateLimiter;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 登录：手机号密码登录或微信登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        // 微信登录的 code 是一次性的，不存在密码撞库，只对手机号密码登录做失败限流
        if (StringUtils.hasText(userLoginDTO.getPhone()) && StringUtils.hasText(userLoginDTO.getPassword())) {
            String account = userLoginDTO.getPhone().trim();
            String ip = request.getRemoteAddr();
            loginRateLimiter.checkAllowed("user", account, ip);

            User user;
            try {
                user = userService.loginByPassword(account, userLoginDTO.getPassword());
                loginRateLimiter.recordSuccess("user", account);
            } catch (RuntimeException e) {
                loginRateLimiter.recordFailure("user", account, ip);
                throw e;
            }
            return Result.success(buildLoginVO(user));
        }

        User user = userService.wxLogin(userLoginDTO);
        return Result.success(buildLoginVO(user));
    }

    /**
     * 手机号注册
     * @param userRegisterDTO
     * @return
     */
    @PostMapping("/register")
    @ApiOperation("手机号注册")
    public Result<UserLoginVO> register(@RequestBody UserRegisterDTO userRegisterDTO, HttpServletRequest request) {
        String account = userRegisterDTO.getPhone() == null ? null : userRegisterDTO.getPhone().trim();
        String ip = request.getRemoteAddr();
        loginRateLimiter.checkAllowed("register", account, ip);

        User user;
        try {
            user = userService.register(userRegisterDTO);
        } finally {
            // 注册成功也计数，防止同一 IP 批量注册
            loginRateLimiter.recordAttempt("register", account, ip);
        }
        return Result.success(buildLoginVO(user));
    }

    /**
     * 退出登录：把当前 token 写入黑名单，立即失效
     * @param request
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("用户退出")
    public Result<String> logout(HttpServletRequest request) {
        tokenBlacklistService.revoke(
                request.getHeader(jwtProperties.getUserTokenName()),
                jwtProperties.getUserSecretKey());
        return Result.success();
    }

    /**
     * 查询当前用户资料
     * @return 用户资料
     */
    @GetMapping("/profile")
    @ApiOperation("用户资料查询")
    public Result<UserProfileVO> profile() {
        return Result.success(userService.getProfile(BaseContext.getCurrentId()));
    }

    /**
     * 更新当前用户资料
     * @param userProfileDTO 昵称、头像、性别
     * @return 更新后的资料
     */
    @PutMapping("/profile")
    @ApiOperation("用户资料更新")
    public Result<UserProfileVO> updateProfile(@RequestBody UserProfileDTO userProfileDTO) {
        return Result.success(userService.updateProfile(BaseContext.getCurrentId(), userProfileDTO));
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
                .phone(user.getPhone())
                .token(token)
                .name(user.getName())
                .avatar(user.getAvatar())
                .sex(user.getSex())
                .build();
    }
}

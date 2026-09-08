package com.qingshan.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.qingshan.constant.MessageConstant;
import com.qingshan.dto.UserLoginDTO;
import com.qingshan.dto.UserRegisterDTO;
import com.qingshan.entity.User;
import com.qingshan.exception.LoginFailedException;
import com.qingshan.mapper.UserMapper;
import com.qingshan.properties.WeChatProperties;
import com.qingshan.service.UserService;
import com.qingshan.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String WX_ACCESS_TOKEN = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String WX_PHONE = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final String PHONE_REGEX = "^1\\d{10}$";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    // 微信 access_token 有调用频率限制，这里做进程内缓存
    // TODO(prod): 多实例部署时每个实例各自缓存，会互相刷新导致 token 失效；
    //   改为存 Redis 并加分布式锁，所有实例共享
    private volatile String accessToken;
    private volatile long accessTokenExpireAt;

    /**
     * 微信登录
     * @param userLoginDTO 登录参数：wx.login 的 code，以及可选的手机号授权数据
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {

        JSONObject session = getSession(userLoginDTO.getCode());
        String openid = session.getString("openid");

        // 手机号 code 或旧版加密数据同时存在时，优先使用新版 code
        String phone = getPhoneNumber(userLoginDTO, session.getString("session_key"));

        User user = userMapper.getByOpenId(openid);

        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .phone(phone)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        } else if (phone != null && !phone.equals(user.getPhone())) {
            // 用户重新授权手机号时更新为最新手机号
            user.setPhone(phone);
            userMapper.update(user);
        }

        return user;
    }

    /**
     * 手机号密码登录
     * @param phone 手机号
     * @param password 密码
     * @return
     */
    @Override
    public User loginByPassword(String phone, String password) {
        checkPhoneAndPassword(phone, password);
        User user = userMapper.getByPhone(phone.trim());
        if (user == null
                || !StringUtils.hasText(user.getPassword())
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new LoginFailedException("手机号或密码错误");
        }
        return user;
    }

    /**
     * 手机号注册
     * @param userRegisterDTO 注册信息
     * @return
     */
    @Override
    public User register(UserRegisterDTO userRegisterDTO) {
        // TODO(prod): 注册/密码重置没有短信验证码，任何人都能用他人手机号注册或占用历史手机号；
        //   上线前接入短信验证码（或仅允许微信手机号授权），并给 user.phone 加唯一索引，
        //   否则并发注册会产生重复手机号，getByPhone 的 limit 1 会随机命中其中一个
        String phone = userRegisterDTO.getPhone() == null ? null : userRegisterDTO.getPhone().trim();
        String password = userRegisterDTO.getPassword();
        checkPhoneAndPassword(phone, password);

        String name = StringUtils.hasText(userRegisterDTO.getName())
                ? userRegisterDTO.getName().trim() : null;

        User exist = userMapper.getByPhone(phone);
        if (exist != null) {
            if (StringUtils.hasText(exist.getPassword())) {
                throw new LoginFailedException("该手机号已注册，请直接登录");
            }
            // 历史微信登录用户首次设置密码
            exist.setPassword(passwordEncoder.encode(password));
            if (name != null) {
                exist.setName(name);
            }
            userMapper.update(exist);
            return exist;
        }

        User user = User.builder()
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .name(name)
                .createTime(LocalDateTime.now())
                .build();
        userMapper.insert(user);
        return user;
    }

    /**
     * 校验手机号和密码格式
     * @param phone 手机号
     * @param password 密码
     */
    private void checkPhoneAndPassword(String phone, String password) {
        if (!StringUtils.hasText(phone) || !phone.trim().matches(PHONE_REGEX)) {
            throw new LoginFailedException("请输入正确的手机号");
        }
        if (!StringUtils.hasText(password) || password.length() < 6 || password.length() > 32) {
            throw new LoginFailedException("密码长度需为6-32位");
        }
    }

    /**
     * 调用微信接口服务，获取 openid 和 session_key
     * @param code wx.login 返回的登录凭证
     * @return
     */
    private JSONObject getSession(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSONObject.parseObject(json);
        if (jsonObject == null || jsonObject.getString("openid") == null) {
            log.error("微信登录失败:{}", json);
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        return jsonObject;
    }

    /**
     * 解析手机号：新版手机号 code 或旧版加密数据
     * @param dto 登录参数
     * @param sessionKey jscode2session 返回的 session_key，旧版解密需要
     * @return 手机号，未授权手机号时返回 null
     */
    private String getPhoneNumber(UserLoginDTO dto, String sessionKey) {
        if (StringUtils.hasText(dto.getPhoneCode())) {
            return getPhoneByCode(dto.getPhoneCode());
        }
        if (StringUtils.hasText(dto.getEncryptedData()) && StringUtils.hasText(dto.getIv())) {
            return getPhoneByEncryptedData(dto.getEncryptedData(), dto.getIv(), sessionKey);
        }
        return null;
    }

    /**
     * 新版基础库：用手机号 code 换取手机号
     * @param phoneCode open-type=getPhoneNumber 返回的 code
     * @return 手机号
     */
    private String getPhoneByCode(String phoneCode) {
        Map<String, String> body = new HashMap<>();
        body.put("code", phoneCode);
        String url = WX_PHONE + "?access_token=" + getAccessToken();
        try {
            String json = HttpClientUtil.doPost4Json(url, body);
            JSONObject jsonObject = JSONObject.parseObject(json);
            if (jsonObject == null || jsonObject.getIntValue("errcode") != 0) {
                log.error("获取微信手机号失败:{}", json);
                throw new LoginFailedException("手机号授权失败，请重试");
            }
            JSONObject phoneInfo = jsonObject.getJSONObject("phone_info");
            if (phoneInfo == null) {
                throw new LoginFailedException("手机号授权失败，请重试");
            }
            return phoneInfo.getString("purePhoneNumber");
        } catch (LoginFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取微信手机号异常", e);
            throw new LoginFailedException("手机号授权失败，请重试");
        }
    }

    /**
     * 旧版基础库：用 session_key 解密 encryptedData 获取手机号
     * @param encryptedData 加密数据
     * @param iv 加密向量
     * @param sessionKey 会话密钥
     * @return 手机号
     */
    private String getPhoneByEncryptedData(String encryptedData, String iv, String sessionKey) {
        if (!StringUtils.hasText(sessionKey)) {
            throw new LoginFailedException("手机号授权失败，请重试");
        }
        try {
            byte[] data = Base64.getDecoder().decode(encryptedData);
            byte[] key = Base64.getDecoder().decode(sessionKey);
            byte[] ivBytes = Base64.getDecoder().decode(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(ivBytes));
            String json = new String(cipher.doFinal(data), StandardCharsets.UTF_8);

            JSONObject jsonObject = JSONObject.parseObject(json);
            JSONObject watermark = jsonObject == null ? null : jsonObject.getJSONObject("watermark");
            if (watermark == null || !weChatProperties.getAppid().equals(watermark.getString("appid"))) {
                throw new LoginFailedException("手机号校验失败，请重试");
            }
            return jsonObject.getString("purePhoneNumber");
        } catch (LoginFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("解密微信手机号失败", e);
            throw new LoginFailedException("手机号授权失败，请重试");
        }
    }

    /**
     * 获取微信 access_token，缓存到过期前 1 分钟
     * @return access_token
     */
    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < accessTokenExpireAt) {
            return accessToken;
        }

        Map<String, String> map = new HashMap<>();
        map.put("grant_type", "client_credential");
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        String json = HttpClientUtil.doGet(WX_ACCESS_TOKEN, map);

        JSONObject jsonObject = JSONObject.parseObject(json);
        String token = jsonObject == null ? null : jsonObject.getString("access_token");
        if (!StringUtils.hasText(token)) {
            log.error("获取微信access_token失败:{}", json);
            throw new LoginFailedException("手机号授权失败，请重试");
        }
        Integer expiresIn = jsonObject.getInteger("expires_in");
        accessToken = token;
        accessTokenExpireAt = now + (expiresIn == null ? 7200 : expiresIn) * 1000L - 60_000L;
        return accessToken;
    }
}

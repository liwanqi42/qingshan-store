package com.qingshan.interceptor;

import com.qingshan.annotation.RequireRole;
import com.qingshan.constant.JwtClaimsConstant;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.entity.Employee;
import com.qingshan.entity.Role;
import com.qingshan.mapper.EmployeeMapper;
import com.qingshan.properties.JwtProperties;
import com.qingshan.service.support.TokenBlacklistService;
import com.qingshan.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * jwt令牌校验的拦截器，同时处理登录状态、首登改密和接口角色校验
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    /** 未修改初始密码时，仅允许访问改密和退出接口 */
    private static final String CHANGE_PASSWORD_PATH = "/admin/employee/editPassword";
    private static final String LOGOUT_PATH = "/admin/employee/logout";

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            if (tokenBlacklistService.isRevoked(claims.getId())) {
                return unauthorized(response);
            }

            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            Employee employee = employeeMapper.getById(empId);
            if (employee == null || StatusConstant.DISABLE.equals(employee.getStatus())) {
                return unauthorized(response);
            }

            // 改密、禁用、角色变更后 tokenVersion 递增，旧 token 立即失效
            Integer claimVersion = toInteger(claims.get(JwtClaimsConstant.TOKEN_VERSION));
            int currentVersion = employee.getTokenVersion() == null ? 0 : employee.getTokenVersion();
            if (claimVersion == null || claimVersion != currentVersion) {
                return unauthorized(response);
            }

            // 首次登录必须先修改初始密码
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (Integer.valueOf(1).equals(employee.getMustChangePassword())
                    && !CHANGE_PASSWORD_PATH.equals(path)
                    && !LOGOUT_PATH.equals(path)) {
                return forbidden(response, "请先修改初始密码");
            }

            // 接口角色校验
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }
            String roleCode = claims.get(JwtClaimsConstant.ROLE_CODE, String.class);
            if (requireRole != null && !hasRole(roleCode, requireRole.value())) {
                return forbidden(response, MessageConstant.NO_PERMISSION);
            }

            log.info("当前员工id：{}", empId);
            BaseContext.setCurrentId(empId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            return unauthorized(response);
        }
    }

    /**
     * 请求结束后清理 ThreadLocal，避免 Tomcat 线程复用时把上一个员工的身份带到下一个请求
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }

    private boolean hasRole(String roleCode, String[] allowedRoles) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        // 超级管理员始终拥有全部权限
        if (Role.CODE_ADMIN.equals(roleCode)) {
            return true;
        }
        for (String allowed : allowedRoles) {
            if (allowed.equals(roleCode)) {
                return true;
            }
        }
        return false;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean unauthorized(HttpServletResponse response) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "登录状态已失效，请重新登录");
        return false;
    }

    private boolean forbidden(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, message);
        return false;
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\"" + message + "\"}");
    }
}

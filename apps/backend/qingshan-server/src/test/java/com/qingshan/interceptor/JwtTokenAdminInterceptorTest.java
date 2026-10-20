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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenAdminInterceptorTest {

    private static final String SECRET = "qingshan-admin-test-secret-0123456789abcdef";
    private static final String TOKEN_HEADER = "token";
    private static final long EMP_ID = 7L;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private JwtTokenAdminInterceptor interceptor;

    private HandlerMethod adminHandler;
    private HandlerMethod openHandler;

    @BeforeEach
    void setUp() throws Exception {
        when(jwtProperties.getAdminTokenName()).thenReturn(TOKEN_HEADER);
        when(jwtProperties.getAdminSecretKey()).thenReturn(SECRET);
        adminHandler = new HandlerMethod(new StubController(),
                StubController.class.getMethod("adminOnly"));
        openHandler = new HandlerMethod(new StubController(),
                StubController.class.getMethod("open"));
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void adminTokenPassesAdminOnlyEndpoint() throws Exception {
        MockHttpServletRequest request = request("/admin/employee/list",
                token(EMP_ID, Role.CODE_ADMIN, 0));
        when(employeeMapper.getById(EMP_ID)).thenReturn(employee(Role.CODE_ADMIN, 0, 0));
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = interceptor.preHandle(request, response, adminHandler);

        assertTrue(passed);
        assertEquals(EMP_ID, BaseContext.getCurrentId());
    }

    @Test
    void employeeTokenGets403OnAdminOnlyEndpoint() throws Exception {
        MockHttpServletRequest request = request("/admin/employee/list",
                token(EMP_ID, Role.CODE_EMPLOYEE, 0));
        when(employeeMapper.getById(EMP_ID)).thenReturn(employee(Role.CODE_EMPLOYEE, 0, 0));
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = interceptor.preHandle(request, response, adminHandler);

        assertFalse(passed);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains(MessageConstant.NO_PERMISSION));
        assertNull(BaseContext.getCurrentId());
    }

    @Test
    void employeeTokenPassesEndpointWithoutRoleRequirement() throws Exception {
        MockHttpServletRequest request = request("/admin/order/page",
                token(EMP_ID, Role.CODE_EMPLOYEE, 0));
        when(employeeMapper.getById(EMP_ID)).thenReturn(employee(Role.CODE_EMPLOYEE, 0, 0));
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, openHandler));
    }

    @Test
    void mustChangePasswordBlocksOtherEndpoints() throws Exception {
        MockHttpServletRequest request = request("/admin/order/page",
                token(EMP_ID, Role.CODE_EMPLOYEE, 0));
        when(employeeMapper.getById(EMP_ID)).thenReturn(employee(Role.CODE_EMPLOYEE, 1, 0));
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = interceptor.preHandle(request, response, openHandler);

        assertFalse(passed);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("请先修改初始密码"));
    }

    @Test
    void revokedTokenGets401() throws Exception {
        MockHttpServletRequest request = request("/admin/employee/list",
                token(EMP_ID, Role.CODE_ADMIN, 0));
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = interceptor.preHandle(request, response, adminHandler);

        assertFalse(passed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void staleTokenVersionGets401AfterPasswordChange() throws Exception {
        MockHttpServletRequest request = request("/admin/employee/list",
                token(EMP_ID, Role.CODE_ADMIN, 0));
        when(employeeMapper.getById(EMP_ID)).thenReturn(employee(Role.CODE_ADMIN, 0, 1));
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = interceptor.preHandle(request, response, adminHandler);

        assertFalse(passed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void disabledEmployeeGets401() throws Exception {
        MockHttpServletRequest request = request("/admin/employee/list",
                token(EMP_ID, Role.CODE_ADMIN, 0));
        Employee disabled = employee(Role.CODE_ADMIN, 0, 0);
        disabled.setStatus(StatusConstant.DISABLE);
        when(employeeMapper.getById(EMP_ID)).thenReturn(disabled);
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, adminHandler));
        assertEquals(401, response.getStatus());
    }

    @Test
    void missingTokenGets401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/employee/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, adminHandler));
        assertEquals(401, response.getStatus());
    }

    private MockHttpServletRequest request(String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader(TOKEN_HEADER, token);
        return request;
    }

    private String token(long empId, String roleCode, int tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, empId);
        claims.put(JwtClaimsConstant.ROLE_CODE, roleCode);
        claims.put(JwtClaimsConstant.TOKEN_VERSION, tokenVersion);
        return JwtUtil.createJWT(SECRET, 60_000, claims);
    }

    private Employee employee(String roleCode, int mustChangePassword, int tokenVersion) {
        return Employee.builder()
                .id(EMP_ID)
                .roleCode(roleCode)
                .status(StatusConstant.ENABLE)
                .mustChangePassword(mustChangePassword)
                .tokenVersion(tokenVersion)
                .build();
    }

    /**
     * 用于构造 HandlerMethod 的测试控制器
     */
    static class StubController {
        @RequireRole(Role.CODE_ADMIN)
        public void adminOnly() {
        }

        public void open() {
        }
    }
}

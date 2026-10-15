package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.constant.JwtClaimsConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.EmployeeDTO;
import com.qingshan.dto.EmployeeLoginDTO;
import com.qingshan.dto.EmployeePageQueryDTO;
import com.qingshan.dto.PasswordEditDTO;
import com.qingshan.entity.Employee;
import com.qingshan.entity.Role;
import com.qingshan.properties.JwtProperties;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.EmployeeService;
import com.qingshan.service.RoleService;
import com.qingshan.service.support.LoginRateLimiter;
import com.qingshan.service.support.TokenBlacklistService;
import com.qingshan.utils.JwtUtil;
import com.qingshan.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private LoginRateLimiter loginRateLimiter;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value = "员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO, HttpServletRequest request) {
        log.info("员工登录：{}", employeeLoginDTO.getUsername());

        String account = employeeLoginDTO.getUsername();
        String ip = request.getRemoteAddr();
        loginRateLimiter.checkAllowed("admin", account, ip);

        Employee employee;
        try {
            employee = employeeService.login(employeeLoginDTO);
            loginRateLimiter.recordSuccess("admin", account);
        } catch (RuntimeException e) {
            loginRateLimiter.recordFailure("admin", account, ip);
            throw e;
        }

        //登录成功后，加载角色与菜单权限
        Role role = roleService.requireEnabledByCode(employee.getRoleCode());

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        claims.put(JwtClaimsConstant.ROLE_CODE, role.getCode());
        claims.put(JwtClaimsConstant.TOKEN_VERSION,
                employee.getTokenVersion() == null ? 0 : employee.getTokenVersion());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .roleCode(role.getCode())
                .roles(Collections.singletonList(role.getCode()))
                .permissions(Role.CODE_ADMIN.equals(role.getCode())
                        ? Collections.singletonList("*")
                        : Collections.emptyList())
                .menuPaths(roleService.resolveMenuPaths(role))
                .mustChangePassword(Integer.valueOf(1).equals(employee.getMustChangePassword()))
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("员工退出")
    public Result<String> logout(HttpServletRequest request) {
        tokenBlacklistService.revoke(
                request.getHeader(jwtProperties.getAdminTokenName()),
                jwtProperties.getAdminSecretKey());
        return Result.success();
    }

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    @PostMapping
    @RequireRole
    @ApiOperation("新增员工")
    public Result<String> save(@RequestBody EmployeeDTO employeeDTO){
        roleService.checkCurrentAdmin();
        return Result.success(employeeService.save(employeeDTO));
    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("员工分页查询")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 启用和禁用员工账户
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @RequireRole
    @ApiOperation("启用和禁用员工账户")
    public Result enableOrDisable(@PathVariable Integer status, long id){
        roleService.checkCurrentAdmin();
        log.info("启用禁用员工账户: {}, {}", status, id);
        employeeService.enableOrDisable(status, id);
        return Result.success();
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(("根据id查询员工信息"))
    public Result<Employee> getBuyId(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    /**
     * 修改员工信息
     * @param employeeDTO
     * @return
     */
    @PutMapping
    @RequireRole
    @ApiOperation("修改员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        roleService.checkCurrentAdmin();
        employeeService.update(employeeDTO);
        return Result.success();
    }

    /**
     * 管理员重置员工密码：返回随机初始密码，员工下次登录必须修改
     * @param id 员工id
     * @return 新的随机初始密码
     */
    @PutMapping("/{id}/reset-password")
    @RequireRole
    @ApiOperation("重置员工密码")
    public Result<String> resetPassword(@PathVariable Long id){
        return Result.success(employeeService.resetPassword(id));
    }

    /**
     * 修改密码
     * @param passwordEditDTO
     * @return
     */
    @PutMapping("/editPassword")
    @ApiOperation("修改密码")
    public Result editPassword(@RequestBody PasswordEditDTO passwordEditDTO){
        //从JWT上下文中获取当前登录员工id，不信任前端传参
        passwordEditDTO.setEmpId(BaseContext.getCurrentId());
        log.info("修改密码，员工id：{}", passwordEditDTO.getEmpId());
        employeeService.editPassword(passwordEditDTO);
        return Result.success();
    }
}

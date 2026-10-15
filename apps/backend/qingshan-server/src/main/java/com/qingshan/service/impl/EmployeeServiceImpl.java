package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.EmployeeDTO;
import com.qingshan.dto.EmployeeLoginDTO;
import com.qingshan.dto.EmployeePageQueryDTO;
import com.qingshan.dto.PasswordEditDTO;
import com.qingshan.entity.Employee;
import com.qingshan.entity.Role;
import com.qingshan.exception.*;
import com.qingshan.mapper.EmployeeMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.EmployeeService;
import com.qingshan.service.RoleService;
import com.qingshan.utils.PageUtil;
import com.qingshan.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private RoleService roleService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 员工登录
     *
     * @param employeeLoginDTO 员工登录参数
     * @return 登录成功的员工信息
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对：兼容历史 MD5，新密码统一使用 BCrypt
        if (!matchesPassword(password, employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        //历史 MD5 密码登录成功后自动升级为 BCrypt
        if (isMd5(employee.getPassword())) {
            Employee upgrade = Employee.builder()
                    .id(employee.getId())
                    .password(passwordEncoder.encode(password))
                    .build();
            employeeMapper.update(upgrade);
            employee.setPassword(upgrade.getPassword());
        }

        if (StatusConstant.DISABLE.equals(employee.getStatus())) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、校验角色是否存在且已启用
        roleService.requireEnabledByCode(employee.getRoleCode());

        if (Integer.valueOf(1).equals(employee.getMustChangePassword())) {
            log.warn("员工{}使用初始密码登录，需先修改密码", employee.getUsername());
        }

        //4、返回实体对象
        return employee;
    }

    /**
     * 新增员工：生成随机初始密码，员工首次登录必须修改
     * @param employeeDTO 员工信息
     * @return 随机初始密码，仅返回一次
     */
    @Override
    public String save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO, employee);

        //设置角色，默认普通员工
        String roleCode = employee.getRoleCode();
        if (!StringUtils.hasText(roleCode)) {
            roleCode = Role.CODE_EMPLOYEE;
        }
        roleService.requireEnabledByCode(roleCode);
        employee.setRoleCode(roleCode);

        //设置账户状态，默认正常状态
        employee.setStatus(StatusConstant.ENABLE);
        employee.setMustChangePassword(1);
        employee.setTokenVersion(0);

        //随机初始密码，通过安全渠道交给员工
        String initialPassword = PasswordUtil.randomPassword(12);
        employee.setPassword(passwordEncoder.encode(initialPassword));

        employeeMapper.insert(employee);
        return initialPassword;
    }

    /**
     * 管理员重置员工密码：生成随机密码，并要求员工下次登录后修改
     * @param id 员工id
     * @return 新的随机初始密码，仅返回一次
     */
    @Override
    @Transactional
    public String resetPassword(Long id) {
        Employee employee = employeeMapper.getById(id);
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        String initialPassword = PasswordUtil.randomPassword(12);
        employeeMapper.resetPassword(id, passwordEncoder.encode(initialPassword));
        return initialPassword;
    }

    /**
     * 分页查询
     * @param employeePageQueryDTO 分页查询参数
     * @return 分页查询结果
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        employeePageQueryDTO.setPage(PageUtil.pageNum(employeePageQueryDTO.getPage()));
        employeePageQueryDTO.setPageSize(PageUtil.pageSize(employeePageQueryDTO.getPageSize()));
        try (Page<Employee> page = PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize())) {
            List<Employee> records = employeeMapper.pageQuery(employeePageQueryDTO);

            long total = page.getTotal();

            //密码一律不下发，身份证号脱敏后再返回
            records.forEach(employee -> {
                employee.setPassword(null);
                maskIdNumber(employee);
            });

            return new PageResult(total, records);
        }
    }

    /**
     * 启用禁用
     * @param status 员工状态，0禁用 1启用
     * @param id 员工id
     */
    @Override
    @Transactional
    public void enableOrDisable(Integer status, long id) {

        Employee employee = Employee.builder()
                                .status(status)
                                .id(id)
                                .build();

        employeeMapper.update(employee);

        // 禁用后立即让该员工已签发的 token 失效
        if (StatusConstant.DISABLE.equals(status)) {
            employeeMapper.increaseTokenVersion(id);
        }
    }

    /**
     * 根据id查询员工信息
     * @param id 员工id
     * @return 员工信息（密码与身份证号已脱敏）
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        //密码与身份证号脱敏
        employee.setPassword("******");
        maskIdNumber(employee);
        return employee;
    }

    /**
     * 修改员工信息
     * @param employeeDTO 员工信息
     */
    @Override
    @Transactional
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

        //前端回显的是脱敏后的身份证号，含*时不更新该字段，避免覆盖数据库中的真实数据
        if (employee.getIdNumber() != null && employee.getIdNumber().contains("*")) {
            employee.setIdNumber(null);
        }

        //校验角色；管理员账号不允许改成其他角色，避免自锁
        boolean roleChanged = false;
        if (StringUtils.hasText(employee.getRoleCode())) {
            Employee existing = employeeMapper.getById(employee.getId());
            if (existing != null
                    && "admin".equals(existing.getUsername())
                    && !employee.getRoleCode().equals(existing.getRoleCode())) {
                throw new BaseException(MessageConstant.ADMIN_EMPLOYEE_ROLE_IMMUTABLE);
            }
            roleChanged = existing != null
                    && !employee.getRoleCode().equals(existing.getRoleCode());
            roleService.requireEnabledByCode(employee.getRoleCode());
        }

        employeeMapper.update(employee);

        // 角色变化后让旧 token 失效，避免继续使用旧角色权限
        if (roleChanged) {
            employeeMapper.increaseTokenVersion(employee.getId());
        }
    }

    /**
     * 兼容历史无盐 MD5 与新 BCrypt 密码校验
     */
    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (isMd5(storedPassword)) {
            String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
            return md5.equalsIgnoreCase(storedPassword);
        }
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    /**
     * 32 位十六进制视为历史 MD5 密码
     */
    private boolean isMd5(String password) {
        return password != null && password.matches("^[a-fA-F0-9]{32}$");
    }

    /**
     * 身份证号脱敏：保留前6位和后4位，中间用*代替
     * @param employee 员工信息，脱敏结果直接写回该对象
     */
    private void maskIdNumber(Employee employee) {
        String idNumber = employee.getIdNumber();
        if (idNumber == null) {
            return;
        }
        int len = idNumber.length();
        // 支持15位、18位身份证脱敏
        if (len == 15 || len == 18) {
            StringBuilder sb = new StringBuilder();
            sb.append(idNumber, 0, 6);
            for (int i = 0; i < len - 10; i++) {
                sb.append('*');
            }
            sb.append(idNumber.substring(len - 4));
            // 将脱敏结果写回对象，否则调用方拿到的仍是原始号码
            employee.setIdNumber(sb.toString());
        }
        // 不符合身份证长度时保持原样
    }

    /**
     * 修改密码
     * @param passwordEditDTO 修改密码参数
     */
    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        //1、根据id查询员工信息
        Employee employee = employeeMapper.getById(passwordEditDTO.getEmpId());
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //2、校验旧密码是否正确（兼容历史 MD5）
        if (!matchesPassword(passwordEditDTO.getOldPassword(), employee.getPassword())) {
            throw new PasswordEditFailedException(MessageConstant.PASSWORD_EDIT_FAILED);
        }

        //3、更新为新密码（BCrypt），清除首登改密标记，并让旧 token 失效
        employeeMapper.updatePasswordAndInvalidate(
                passwordEditDTO.getEmpId(),
                passwordEncoder.encode(passwordEditDTO.getNewPassword()));
    }

}

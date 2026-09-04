package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.PasswordConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.EmployeeDTO;
import com.qingshan.dto.EmployeeLoginDTO;
import com.qingshan.dto.EmployeePageQueryDTO;
import com.qingshan.dto.PasswordEditDTO;
import com.qingshan.entity.Employee;
import com.qingshan.exception.AccountLockedException;
import com.qingshan.exception.AccountNotFoundException;
import com.qingshan.exception.PasswordEditFailedException;
import com.qingshan.exception.PasswordErrorException;
import com.qingshan.mapper.EmployeeMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.EmployeeService;
import com.qingshan.utils.PageUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

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

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO 员工信息
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO, employee);

        //设置账户状态，默认正常状态
        employee.setStatus(StatusConstant.ENABLE);

        //设置密码
        // TODO(prod): 不要使用统一的默认密码，改为随机初始密码并通过安全渠道发给员工
        employee.setPassword(passwordEncoder.encode(PasswordConstant.DEFAULT_PASSWORD));


        employeeMapper.insert(employee);
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
    public void enableOrDisable(Integer status, long id) {

        Employee employee = Employee.builder()
                                .status(status)
                                .id(id)
                                .build();

        employeeMapper.update(employee);
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
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

        //前端回显的是脱敏后的身份证号，含*时不更新该字段，避免覆盖数据库中的真实数据
        if (employee.getIdNumber() != null && employee.getIdNumber().contains("*")) {
            employee.setIdNumber(null);
        }

        employeeMapper.update(employee);
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

        //3、更新为新密码（BCrypt）
        Employee emp = Employee.builder()
                .id(passwordEditDTO.getEmpId())
                .password(passwordEncoder.encode(passwordEditDTO.getNewPassword()))
//                .updateTime(LocalDateTime.now())
//                .updateUser(BaseContext.getCurrentId())
                .build();

        employeeMapper.update(emp);
    }

}

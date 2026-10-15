package com.qingshan.service;

import com.qingshan.dto.EmployeeDTO;
import com.qingshan.dto.EmployeeLoginDTO;
import com.qingshan.dto.EmployeePageQueryDTO;
import com.qingshan.dto.PasswordEditDTO;
import com.qingshan.entity.Employee;
import com.qingshan.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO 员工信息
     * @return 随机初始密码，仅返回一次
     */
    String save(EmployeeDTO employeeDTO);

    /**
     * 管理员重置员工密码
     * @param id 员工id
     * @return 新的随机初始密码，仅返回一次
     */
    String resetPassword(Long id);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用禁用员工账户
     * @param status
     * @param id
     */
    void enableOrDisable(Integer status, long id);

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    Employee getById(Long id);

    /**
     * 修改员工信息
     * @param employeeDTO
     */
    void update(EmployeeDTO employeeDTO);

    /**
     * 修改密码
     * @param passwordEditDTO
     */
    void editPassword(PasswordEditDTO passwordEditDTO);
}

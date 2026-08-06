package com.qingshan.service;

import com.qingshan.dto.EmployeeDTO;
import com.qingshan.dto.EmployeeLoginDTO;
import com.qingshan.dto.EmployeePageQueryDTO;
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
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);
}

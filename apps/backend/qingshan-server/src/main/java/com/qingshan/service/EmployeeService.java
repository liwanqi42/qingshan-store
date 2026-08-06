package com.qingshan.service;

import com.qingshan.dto.EmployeeLoginDTO;
import com.qingshan.entity.Employee;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

}

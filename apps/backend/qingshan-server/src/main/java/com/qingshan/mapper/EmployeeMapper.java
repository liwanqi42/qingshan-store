package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.annotation.AutoFill;
import com.qingshan.dto.EmployeePageQueryDTO;
import com.qingshan.entity.Employee;
import com.qingshan.enumeration.OperationType;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EmployeeMapper {



    /**
     * 根据用户名查询员工
     * @param username 用户名
     * @return 员工
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 插入员工数据
     * @param employee 员工信息
     */
    @Insert("insert into employee(name, username, password, phone, sex, id_number, role_code, status, must_change_password, token_version, create_time, update_time, create_user, update_user)" +
            "values " +
            "(#{name}, #{username}, #{password}, #{phone}, #{sex}, #{idNumber}, #{roleCode}, #{status}, #{mustChangePassword}, #{tokenVersion}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Employee employee);

    /**
     * 分页查询
     * @param employeePageQueryDTO 分页查询参数
     * @return 分页数据
     */
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 根据主键更新属性
     * @param employee 员工信息
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);

    /**
     * 根据id查询员工信息
     * @param id 员工id
     * @return 员工信息
     */
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);

    /**
     * 统计使用指定角色的员工数量
     * @param roleCode 角色编码
     * @return 员工数量
     */
    @Select("select count(*) from employee where role_code = #{roleCode}")
    int countByRoleCode(String roleCode);

    /**
     * 递增员工令牌版本，使该员工已签发的 token 全部失效
     */
    @Update("update employee set token_version = token_version + 1 where id = #{id}")
    int increaseTokenVersion(@Param("id") Long id);

    /**
     * 递增某角色下所有员工的令牌版本，角色被禁用时调用
     */
    @Update("update employee set token_version = token_version + 1 where role_code = #{roleCode}")
    int increaseTokenVersionByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 修改密码：清除首登改密标记，并让旧 token 失效
     */
    @Update("update employee set password = #{password}, must_change_password = 0, " +
            "token_version = token_version + 1, update_time = now() where id = #{id}")
    int updatePasswordAndInvalidate(@Param("id") Long id, @Param("password") String password);

    /**
     * 管理员重置密码：设置首登改密标记，并让旧 token 失效
     */
    @Update("update employee set password = #{password}, must_change_password = 1, " +
            "token_version = token_version + 1, update_time = now() where id = #{id}")
    int resetPassword(@Param("id") Long id, @Param("password") String password);
}

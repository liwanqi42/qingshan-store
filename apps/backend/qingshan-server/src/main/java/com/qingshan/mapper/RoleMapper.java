package com.qingshan.mapper;

import com.qingshan.annotation.AutoFill;
import com.qingshan.entity.Role;
import com.qingshan.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper {

    /**
     * 查询角色列表，status 为空时查询全部
     * @param status 状态 0:禁用，1:启用
     * @return 角色列表
     */
    List<Role> list(@Param("status") Integer status);

    /**
     * 根据id查询角色
     * @param id 角色id
     * @return 角色
     */
    @Select("select * from sys_role where id = #{id}")
    Role getById(Long id);

    /**
     * 根据编码查询角色
     * @param code 角色编码
     * @return 角色
     */
    @Select("select * from sys_role where code = #{code}")
    Role getByCode(String code);

    /**
     * 新增角色
     * @param role 角色信息
     */
    @Insert("insert into sys_role(name, code, status, menu_paths, create_time, update_time, create_user, update_user)" +
            " values " +
            "(#{name}, #{code}, #{status}, #{menuPaths}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Role role);

    /**
     * 根据id更新角色属性
     * @param role 角色信息
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Role role);

    /**
     * 根据id删除角色
     * @param id 角色id
     */
    @Delete("delete from sys_role where id = #{id}")
    void deleteById(Long id);
}

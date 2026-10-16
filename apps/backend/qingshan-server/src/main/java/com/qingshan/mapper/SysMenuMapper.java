package com.qingshan.mapper;

import com.qingshan.annotation.AutoFill;
import com.qingshan.entity.SysMenu;
import com.qingshan.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper {

    /**
     * 查询全部菜单配置
     */
    List<SysMenu> listAll();

    @Select("select * from sys_menu where id = #{id}")
    SysMenu getById(Long id);

    @Select("select * from sys_menu where name = #{name}")
    SysMenu getByName(String name);

    @Insert("insert into sys_menu(name, path, title, icon, sort, hidden, more, " +
            "create_time, update_time, create_user, update_user) values " +
            "(#{name}, #{path}, #{title}, #{icon}, #{sort}, #{hidden}, #{more}, " +
            "#{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(OperationType.INSERT)
    void insert(SysMenu menu);

    @AutoFill(OperationType.UPDATE)
    void update(SysMenu menu);

    @Delete("delete from sys_menu where id = #{id}")
    void deleteById(Long id);

    @Delete("delete from sys_menu")
    void deleteAll();
}

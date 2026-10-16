package com.qingshan.mapper;

import com.qingshan.annotation.AutoFill;
import com.qingshan.entity.SysDictType;
import com.qingshan.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDictTypeMapper {

    /**
     * 查询全部字典类型，按排序值升序
     */
    List<SysDictType> listAll();

    /**
     * 查询启用状态的字典类型
     */
    List<SysDictType> listEnabled();

    @Select("select * from sys_dict_type where id = #{id}")
    SysDictType getById(Long id);

    @Select("select * from sys_dict_type where type = #{type}")
    SysDictType getByType(String type);

    @Insert("insert into sys_dict_type(type, name, status, sort, is_system, " +
            "create_time, update_time, create_user, update_user) values " +
            "(#{type}, #{name}, #{status}, #{sort}, #{isSystem}, " +
            "#{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(OperationType.INSERT)
    void insert(SysDictType dictType);

    @AutoFill(OperationType.UPDATE)
    void update(SysDictType dictType);

    @Delete("delete from sys_dict_type where id = #{id}")
    void deleteById(Long id);

    @Delete("delete from sys_dict_type")
    void deleteAll();
}

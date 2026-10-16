package com.qingshan.mapper;

import com.qingshan.annotation.AutoFill;
import com.qingshan.entity.SysDictItem;
import com.qingshan.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDictItemMapper {

    /**
     * 查询全部字典项
     */
    List<SysDictItem> listAll();

    /**
     * 查询启用状态的字典项
     */
    List<SysDictItem> listEnabled();

    @Select("select * from sys_dict_item where id = #{id}")
    SysDictItem getById(Long id);

    @Select("select * from sys_dict_item where type_id = #{typeId} and value = #{value}")
    SysDictItem getByTypeIdAndValue(@Param("typeId") Long typeId, @Param("value") String value);

    @Insert("insert into sys_dict_item(type_id, label, value, tag_type, sort, status, " +
            "create_time, update_time, create_user, update_user) values " +
            "(#{typeId}, #{label}, #{value}, #{tagType}, #{sort}, #{status}, " +
            "#{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(OperationType.INSERT)
    void insert(SysDictItem dictItem);

    @AutoFill(OperationType.UPDATE)
    void update(SysDictItem dictItem);

    @Delete("delete from sys_dict_item where id = #{id}")
    void deleteById(Long id);

    @Delete("delete from sys_dict_item where type_id = #{typeId}")
    void deleteByTypeId(Long typeId);

    @Delete("delete from sys_dict_item")
    void deleteAll();
}

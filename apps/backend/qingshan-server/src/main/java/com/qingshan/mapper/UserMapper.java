package com.qingshan.mapper;

import com.qingshan.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    @Select("select * from user where id = #{id}")
    User getById(Long id);

    @Select("select * from user where openid = #{openid}")
    User getByOpenId(String openid);

    @Select("select * from user where phone = #{phone} limit 1")
    User getByPhone(String phone);

    void insert(User user);

    void update(User user);

}

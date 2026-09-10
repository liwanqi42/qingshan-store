package com.qingshan.mapper;

import com.qingshan.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    /**
     * 根据条件统计用户数量
     * @param begin
     * @param end
     * @return
     */
    Integer countByCondition(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("select * from `user` where id = #{id}")
    User getById(Long id);

    @Select("select * from `user` where openid = #{openid}")
    User getByOpenId(String openid);

    @Select("select * from `user` where phone = #{phone} limit 1")
    User getByPhone(String phone);

    void insert(User user);

    void update(User user);

}

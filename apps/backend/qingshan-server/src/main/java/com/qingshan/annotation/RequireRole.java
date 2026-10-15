package com.qingshan.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端接口角色要求，默认仅超级管理员可访问。
 * 超级管理员（admin）始终放行。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色编码
     */
    String[] value() default {"admin"};
}

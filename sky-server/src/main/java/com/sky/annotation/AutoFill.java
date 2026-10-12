package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)  // 限制这个注解只能用于注解方法（在方法上使用）
@Retention(RetentionPolicy.RUNTIME)     //  TODO 了解这个注解的作用
public @interface AutoFill {
    // 定义数据库操作类型：因为我们需要根据数据库的操作类型（insert or update 来判断需要自动填充哪些字段）
    OperationType value();
}

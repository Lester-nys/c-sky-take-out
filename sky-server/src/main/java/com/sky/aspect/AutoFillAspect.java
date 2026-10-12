package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**实现公共字段自动填充的方法
 * 项目中使用 Spring AOP 实现公共字段自动填充。通过自定义 @AutoFill 注解标识需要自动填充的方法，在切面类中使用 @Pointcut 定义切入点，通过 @Before 前置通知拦截目标方法。利用 JoinPoint 获取方法参数中的实体对象，然后通过反射调用 setter 方法，为 createTime、createUser、updateTime、updateUser 等公共字段赋值。
 * */
/**
 * 自定义切面：实现公共字段自动填充处理逻辑
 * */
// TODO 了解这些注解的作用
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    // TODO 什么是切入点？又是干嘛的？里面的execution是用来做什么的？
    /**
     * 切入点
     * */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}


    // TODO 什么是前置通知？ 这是干嘛的？
    /**
     * 前置通知：在通知中进行公共字段的赋值
     * */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint)
    {
        log.info("开始进行公共字段自动填充......");
        // 获取当前被拦截方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();     // 方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);    // 获得方法上的注解对象
        OperationType operationType = autoFill.value();                             // 获得数据库操作类型

        // 获取当前被拦截方法的参数  --  实体对象
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0)
            return;

        Object entity = args[0];        // 提前约定好的

        // 准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 根据当前不同的操作类型，为对应的属性通过反射赋值
        if(operationType == OperationType.INSERT)
        {
            try
            {
                Method setCreatTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);   //  TODO 后面一个参数是什么呢

                // 通过反射为公共字段赋值
                setCreatTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
        else if(operationType == OperationType.UPDATE)
        {
            try
            {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                // 通过反射为公共字段赋值
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

}

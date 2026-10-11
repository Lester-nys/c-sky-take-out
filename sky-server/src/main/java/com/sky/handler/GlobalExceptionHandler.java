package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.Servlet;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final Servlet servlet;

    public GlobalExceptionHandler(Servlet servlet) {
        this.servlet = servlet;
    }

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionalHandler(java.sql.SQLIntegrityConstraintViolationException ex)
    {
        //  Duplicate entry 'zhagnsan' for key 'employee.idx_username'  异常信息
        String message = ex.getMessage();
        if(message.contains("Duplicate entry"))
        {
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }else
        {
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

}

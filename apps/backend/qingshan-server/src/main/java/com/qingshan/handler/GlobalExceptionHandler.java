package com.qingshan.handler;

import com.qingshan.constant.MessageConstant;
import com.qingshan.exception.BaseException;
import com.qingshan.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public Result exceptionHandler(BaseException ex){

        log.warn("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理数据库约束异常（MyBatis-Spring 通常翻译成 DataIntegrityViolationException）
     * 不返回原始 SQL 信息，避免泄露表名、索引名等结构
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result exceptionHandler(SQLIntegrityConstraintViolationException exception){

        log.error("数据库约束异常", exception);
        return Result.error(MessageConstant.DATA_CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result exceptionHandler(DataIntegrityViolationException exception){

        log.error("数据库约束异常", exception);
        return Result.error(MessageConstant.DATA_CONFLICT);
    }

    /**
     * 上传文件超过 spring.servlet.multipart 限制时，返回明确提示
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result exceptionHandler(MaxUploadSizeExceededException exception){

        log.warn("上传文件超过大小限制", exception);
        return Result.error(MessageConstant.UPLOAD_SIZE_EXCEEDED);
    }

    /**
     * 兜底处理未预期异常，统一返回系统繁忙，避免堆栈/SQL 细节返回给前端
     */
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception exception){

        log.error("系统异常", exception);
        return Result.error(MessageConstant.SYSTEM_ERROR);
    }
}

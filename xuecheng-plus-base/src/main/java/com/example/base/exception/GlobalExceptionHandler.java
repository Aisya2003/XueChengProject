package com.example.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ControllerAdvice//控制器增强
@Slf4j
public class GlobalExceptionHandler{
    //处理自定义XuechengPlusException，可预知异常
    @ResponseBody//返回为JSON
    @ExceptionHandler(XuechengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)//定义返回的异常Code 500
    public RestErrorResponse handleXuechengPlusException(XuechengPlusException e){


        String errMessage = e.getErrMessage();

        log.error("捕获异常消息{}",errMessage);

        e.printStackTrace();

        return new RestErrorResponse(errMessage);

    }

    //JSR303校验抛出的异常
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        //获取所有的异常
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        //定义StringBuffer收集异常参数
        StringBuffer errStr = new StringBuffer();
        fieldErrors.forEach(fieldError -> {
            errStr.append(fieldError.getDefaultMessage()).append(",");
        });
        return new RestErrorResponse(errStr.toString());

    }

    //不可预知异常
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse handleException(Exception e){


        log.error("捕获异常消息{}",e.getMessage());

        e.printStackTrace();

        //对不可预知异常统一返回信息
        return new RestErrorResponse(CommonError.UNKNOWN_ERROR.getErrMessage());

    }
}

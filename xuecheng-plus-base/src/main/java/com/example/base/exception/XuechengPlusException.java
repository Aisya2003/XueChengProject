package com.example.base.exception;

import lombok.Data;

@Data
public class XuechengPlusException extends RuntimeException{
    private String errMessage;
    public XuechengPlusException() {
        super();
    }

    public XuechengPlusException(String message) {
        super(message);
        this.errMessage = message;
    }

    public static void cast(String errMessage){
        throw new XuechengPlusException(errMessage);
    }
    public static void cast(CommonError commonError){
        throw new XuechengPlusException(commonError.getErrMessage());
    }
}

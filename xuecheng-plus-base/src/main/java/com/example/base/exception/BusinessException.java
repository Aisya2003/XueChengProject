package com.example.base.exception;

import lombok.Data;

@Data
public class BusinessException extends RuntimeException {
    private String errMessage;

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
        this.errMessage = message;
    }

    public static void cast(String errMessage) {
        throw new BusinessException(errMessage);
    }

    public static void cast(CommonError commonError) {
        throw new BusinessException(commonError.getErrMessage());
    }
}

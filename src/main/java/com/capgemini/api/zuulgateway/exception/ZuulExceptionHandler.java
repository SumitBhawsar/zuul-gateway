package com.capgemini.api.zuulgateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ZuulExceptionHandler {

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(RuntimeException.class)
    public String handlerRuntimeException(RuntimeException exc){
        return "{\"error\" : \""+exc.getMessage() + "\"}";
    }
}

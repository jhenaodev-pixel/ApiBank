package com.henaobank.bankapplication.exception;


public class BadRequestException  extends RuntimeException {

    public BadRequestException(String message){
        super(message);
    }
}

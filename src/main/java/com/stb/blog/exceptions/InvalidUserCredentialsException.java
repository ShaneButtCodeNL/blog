package com.stb.blog.exceptions;

public class InvalidUserCredentialsException extends Exception{
    public InvalidUserCredentialsException(String msg){
        super(msg);
    }
}

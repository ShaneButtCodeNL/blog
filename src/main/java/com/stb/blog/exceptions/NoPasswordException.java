package com.stb.blog.exceptions;

public class NoPasswordException extends InvalidUserCredentialsException{
    public NoPasswordException(String msg){
        super(msg);
    }
}

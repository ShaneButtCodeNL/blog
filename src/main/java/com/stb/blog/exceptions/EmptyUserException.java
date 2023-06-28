package com.stb.blog.exceptions;

public class EmptyUserException extends InvalidUserCredentialsException{
    public EmptyUserException(String msg){
        super(msg);
    }
}

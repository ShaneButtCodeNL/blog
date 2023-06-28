package com.stb.blog.exceptions;

public class NoUsernameException extends InvalidUserCredentialsException{
    public NoUsernameException(String msg){
        super(msg);
    }
}

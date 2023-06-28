package com.stb.blog.exceptions;

public class UserExistsException extends Exception{
    public UserExistsException(String message){
        super(message);
    }
}

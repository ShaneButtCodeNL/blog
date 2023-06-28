package com.stb.blog.exceptions;

public class JwtTokenException extends Exception{
    public JwtTokenException(String msg){
        super(msg);
    }
}

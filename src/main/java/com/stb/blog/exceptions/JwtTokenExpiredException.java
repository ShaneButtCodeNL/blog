package com.stb.blog.exceptions;

public class JwtTokenExpiredException extends JwtTokenException{
    public JwtTokenExpiredException(String msg){
        super(msg);
    }
}

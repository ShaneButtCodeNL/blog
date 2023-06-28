package com.stb.blog.exceptions;

public class JwtTokenNotFoundException extends JwtTokenException{
    public JwtTokenNotFoundException(String msg){
        super(msg);
    }
}

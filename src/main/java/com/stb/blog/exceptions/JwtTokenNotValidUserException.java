package com.stb.blog.exceptions;

public class JwtTokenNotValidUserException extends JwtTokenException{
    public JwtTokenNotValidUserException(String msg){
        super(msg);
    }
}

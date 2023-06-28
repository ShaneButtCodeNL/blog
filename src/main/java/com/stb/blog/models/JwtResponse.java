package com.stb.blog.models;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
public class JwtResponse implements Serializable {
    private static final long serialVersionUID=-8091879091924046844L;
    private final String jwtToken;

    public String getJwtToken(){
        return this.jwtToken;
    }
}
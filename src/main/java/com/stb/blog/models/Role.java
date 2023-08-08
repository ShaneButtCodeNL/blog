package com.stb.blog.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Role {
    private String name;

    @Override
    public String toString(){
        return name;
    }

    public boolean isEqualToString(String role){
        return name.equals(role);
    }
}

package com.stb.blog.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserReturn {
    private String userId;
    private String username;
    private Set<String> roles;
    private Date createdOn;
    private boolean isBanned;
    private boolean isDisabled;
}

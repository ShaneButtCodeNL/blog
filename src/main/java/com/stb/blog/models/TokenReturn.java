package com.stb.blog.models;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TokenReturn {
    String token;
    Date expires;
}

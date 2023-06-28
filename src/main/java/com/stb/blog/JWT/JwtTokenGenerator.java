package com.stb.blog.JWT;

import com.stb.blog.models.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenGenerator {

    @Value("${jwt.secret}")
    private String secret;

    private Key getKey(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
    public String createToken(User user){
        Date iss = new Date();
        int ONE_DAY = 1000 * 60 * 60 * 24;
        Date exp = new Date(new Date().getTime() + ONE_DAY);
        return Jwts.builder().setIssuer("SERVER").setSubject(user.getUsername()).setExpiration(exp).setIssuedAt(iss).claim("role",user.getRoles().toString()).signWith(getKey()).compact();
    }
}
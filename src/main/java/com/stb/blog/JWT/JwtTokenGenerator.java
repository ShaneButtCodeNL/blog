package com.stb.blog.JWT;

import com.stb.blog.models.TokenReturn;
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
    private final int HALF_HOUR = 1000 * 60 * 30;
    private final int ONE_DAY = 1000 * 60 * 60 * 24;


    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.refresh.secret}")
    private String refreshSecret;

    private Key getKey(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
    private Key getRefreshKey(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
    }
    public TokenReturn createAccessToken(User user){
        Date iss = new Date();
        Date exp = new Date(new Date().getTime() + HALF_HOUR);
        return new TokenReturn(Jwts.builder()
                .setIssuer("SERVER")
                .setSubject(user.getUsername())
                .setExpiration(exp)
                .setIssuedAt(iss)
                .claim("role",user.getRoles().toString()).signWith(getKey()).compact(),exp);
    }
    public TokenReturn createRefreshToken(User user){
        Date iss = new Date();
        Date exp = new Date(new Date().getTime() + ONE_DAY);
        return new TokenReturn(Jwts.builder()
                .setIssuer("SERVER")
                .setSubject(user.getUsername())
                .setExpiration(exp)
                .setIssuedAt(iss)
                .signWith(getRefreshKey()).compact(),exp);
    }
}
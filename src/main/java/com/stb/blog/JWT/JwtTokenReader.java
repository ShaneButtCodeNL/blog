package com.stb.blog.JWT;


import com.stb.blog.exceptions.JwtTokenException;
import com.stb.blog.exceptions.JwtTokenExpiredException;
import com.stb.blog.exceptions.JwtTokenNotFoundException;
import com.stb.blog.exceptions.JwtTokenNotValidUserException;
import com.stb.blog.models.User;
import com.stb.blog.services.UserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenReader {

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


    @Autowired
    UserService userService;


    public boolean verifyAccessToken(String jwtString) throws JwtTokenException {
        Date currDate = new Date();
        Jws<Claims> claims;
        try {
            claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(jwtString);
        } catch (SignatureException e) {
            throw new JwtTokenNotFoundException("JWT not found.");
        } catch (ExpiredJwtException e){
            throw new JwtTokenExpiredException("Token expired");
        }
        Claims body = claims.getBody();
        String subject = body.getSubject();
        Date exp = body.getExpiration();
        if (currDate.before(exp)) {
            User requestUser = userService.findUserByUsername(subject);
            if (requestUser.getUsername().equalsIgnoreCase(subject)) {
                return true;
            }else{
                throw new JwtTokenNotValidUserException("This user doesn't have authorization.");
            }
        }else{
            throw new JwtTokenExpiredException("JWT is expired.");
        }
    }

    public boolean verifyRefreshToken(String jwtString) throws JwtTokenException {
        Date currDate = new Date();
        Jws<Claims> claims;
        try {
            claims = Jwts.parserBuilder().setSigningKey(refreshSecret).build().parseClaimsJws(jwtString);
        } catch (SignatureException e) {
            throw new JwtTokenNotFoundException("JWT not found.");
        } catch (ExpiredJwtException e){
            throw new JwtTokenExpiredException("Token expired");
        }
        Claims body = claims.getBody();
        String subject = body.getSubject();
        Date exp = body.getExpiration();
        if (currDate.before(exp)) {
            User requestUser = userService.findUserByUsername(subject);
            if (requestUser.getUsername().equalsIgnoreCase(subject)) {
                return true;
            }else{
                throw new JwtTokenNotValidUserException("This user doesn't have authorization.");
            }
        }else{
            throw new JwtTokenExpiredException("JWT is expired.");
        }
    }

    public String getUsernameFromAccessToken(String jwtString) throws JwtTokenException{
        Claims claims;
        try {
            claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(jwtString).getBody();
        } catch (SignatureException e){
            throw new JwtTokenNotFoundException("JWT not found.");
        }
        return claims.getSubject();
    }

    public String getUsernameFromRefreshToken(String jwtString) throws JwtTokenException{
        Claims claims;
        try {
            claims = Jwts.parserBuilder().setSigningKey(refreshSecret).build().parseClaimsJws(jwtString).getBody();
        } catch (SignatureException e){
            throw new JwtTokenNotFoundException("JWT not found.");
        }
        return claims.getSubject();
    }

    public String getRolesFromAccessToken(String jwtString) throws JwtTokenException{
        Claims claims;
        try{
            claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(jwtString).getBody();
        }catch (SignatureException e){
            throw new JwtTokenNotFoundException("JWT not found.");
        }
        return claims.get("roles").toString();
    }
}
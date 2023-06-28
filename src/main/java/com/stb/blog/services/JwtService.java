package com.stb.blog.services;

import com.stb.blog.JWT.JwtTokenGenerator;
import com.stb.blog.JWT.JwtTokenReader;
import com.stb.blog.exceptions.*;
import com.stb.blog.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JwtService {

    private final String BEARER="Bearer ";
    @Autowired
    JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    JwtTokenReader jwtTokenReader;

    /**
     * Generates a JWT.
     * @param user The user to get a token for
     * @return The JWT String for this user if valid.
     * @throws InvalidUserCredentialsException If user is null or if "username" or "password" are empty strings
     */
    public String generateToken(User user) throws InvalidUserCredentialsException {
        if(user==null) throw new EmptyUserException("User Doesn't Exist.");
        if(user.getUsername().equalsIgnoreCase(""))throw new NoUsernameException("Username is empty.");
        if(user.getPassword().equalsIgnoreCase(""))throw new NoPasswordException("Password is empty.");
        String token= jwtTokenGenerator.createToken(user);
        return token;
    }

    /**
     * Checks if JWT is valid. Sent by proper user and token is not expired
     * @param token A string representation of a JWT
     * @return Is the token valid
     */
    public boolean validateToken(String token){
        boolean isValid=false;
        try{
            isValid=jwtTokenReader.verifyJwtString(token);
        }catch (JwtTokenException e){
            System.out.println(e);
            return false;
        }
        return isValid;
    }

    public String getUsernameFromToken(String token){
        String name=null;
        try {
            name=jwtTokenReader.getUsernameFromToken(token);
        }catch(JwtTokenException e){
            System.out.println(e.getMessage());
            return null;
        }
        return name;
    }

    public List<String> getRolesFromToken(String token){
        List<String> roles = new ArrayList<>();
        try {
            String roleString = jwtTokenReader.getRolesFromToken(token);
            roleString=roleString.replace("[","").replace("]","");
            var itter = roleString.split(",");
            for(var role :itter){
                roles.add(role);
            }
        } catch (JwtTokenException e){
            System.out.println(e.getMessage());
            return null;
        }
        return roles;
    }

    public String getJWTFromBearerToken(String bearerToken) throws JwtTokenException{
        String token;
        if(bearerToken.startsWith(BEARER)){
                token = bearerToken.substring(BEARER.length());
                if(!validateToken(token)) throw new JwtTokenException("JWT is not valid");
        }else throw new JwtTokenException("Bearer token must start with \""+BEARER+"\"");
        return token;
    }
}

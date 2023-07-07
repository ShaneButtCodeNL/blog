package com.stb.blog.controllers;

import com.stb.blog.exceptions.InvalidUserCredentialsException;
import com.stb.blog.models.Role;
import com.stb.blog.models.User;
import com.stb.blog.models.UserReturn;
import com.stb.blog.services.JwtService;
import com.stb.blog.services.UserService;
import jakarta.annotation.security.RolesAllowed;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Date;
import java.util.HashSet;
import java.util.Map;

@RestController
@RequestMapping("/api/blog/users")
public class UserController{
    @Autowired
    UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,Object> payload){
        if(!payload.containsKey("username") || !payload.containsKey("password")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username = payload.get("username").toString();
        String password = payload.get("password").toString();

        var user = userService.findUserByUsernameAndPassword(username,password);
        String jwtString;
        try{
            jwtString=jwtService.generateToken(user);
        } catch(InvalidUserCredentialsException e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_FOUND);
        }
        updateUserLastAccess(user);
        return new ResponseEntity<>(new LoginReturn(jwtString,user.getUserReturn()),HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String,Object> payload){
        Date dateNow = new Date();
        if(!payload.containsKey("username") || !payload.containsKey("password")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username = payload.get("username").toString();
        String password = payload.get("password").toString();

        var userWithUsername = userService.findUserByUsername(username);
        if(userWithUsername != null){
            return new ResponseEntity<>(null,HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUserId(new ObjectId().toHexString());
        user.setLastAccess(dateNow);
        user.setCreatedOn(dateNow);
        user.setRoles(new HashSet<Role>());
        user.addRole("ROLE_USER");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        String jwtString;
        try{
            jwtString=jwtService.generateToken(user);
        }catch(InvalidUserCredentialsException e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        }
        userService.saveNewUser(user);
        return new ResponseEntity<>(new LoginReturn(jwtString,user.getUserReturn()),HttpStatus.CREATED);
    }


    @PutMapping("/add-auth/admin")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
    public ResponseEntity<?> addUserRoleAdmin(@RequestBody Map<String,Object> payload){
        Date dateNow = new Date();
        if( !payload.containsKey("username") ){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username= payload.get("username").toString();
        String newRole = "ROLE_ADMIN";
        User userToChange = userService.findUserByUsername(username);
        if(userToChange == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        userToChange.addRole(newRole);
        var saved = userService.saveNewUser(userToChange);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/add-auth/writer")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
    public ResponseEntity<?> addUserRoleWriter(@RequestBody Map<String,Object> payload){
        Date dateNow = new Date();
        if( !payload.containsKey("username") ){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username= payload.get("username").toString();
        String newRole = "ROLE_WRITER";
        User userToChange = userService.findUserByUsername(username);
        if(userToChange == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        userToChange.addRole(newRole);
        var saved = userService.saveNewUser(userToChange);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/add-auth/moderator")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
    public ResponseEntity<?> addUserRoleModerator(@RequestBody Map<String,Object> payload){
        Date dateNow = new Date();
        if( !payload.containsKey("username") ){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username= payload.get("username").toString();
        String newRole = "ROLE_MODERATOR";
        User userToChange = userService.findUserByUsername(username);
        if(userToChange == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        userToChange.addRole(newRole);
        var saved = userService.saveNewUser(userToChange);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/remove-auth/admin")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
    public ResponseEntity<?> removeUserRoleAdmin(@RequestBody Map<String,Object> payload){
        Date dateNow = new Date();
        if( !payload.containsKey("username") ){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username= payload.get("username").toString();
        String newRole = "ROLE_ADMIN";
        User userToChange = userService.findUserByUsername(username);
        if(userToChange == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        userToChange.removeRole(newRole);
        var saved = userService.saveNewUser(userToChange);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/remove-auth/writer")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
    public ResponseEntity<?> removeUserRoleWriter(@RequestBody Map<String,Object> payload){
        Date dateNow = new Date();
        if( !payload.containsKey("username") ){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username= payload.get("username").toString();
        String newRole = "ROLE_WRITER";
        User userToChange = userService.findUserByUsername(username);
        if(userToChange == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        userToChange.removeRole(newRole);
        var saved = userService.saveNewUser(userToChange);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/remove-auth/moderator")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
    public ResponseEntity<?> removeUserRoleModerator(@RequestBody Map<String,Object> payload){
        Date dateNow = new Date();
        if( !payload.containsKey("username") ){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username= payload.get("username").toString();
        String newRole = "ROLE_MODERATOR";
        User userToChange = userService.findUserByUsername(username);
        if(userToChange == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        userToChange.removeRole(newRole);
        var saved = userService.saveNewUser(userToChange);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }



    private User updateUserLastAccess(User user){
        Date dateNow = new Date();
        user.setLastAccess(dateNow);
        var saved = userService.saveNewUser(user);
        return saved;
    }

}

record LoginReturn(String token, UserReturn details){}

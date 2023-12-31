package com.stb.blog.controllers;

import com.stb.blog.exceptions.InvalidUserCredentialsException;
import com.stb.blog.models.Role;
import com.stb.blog.models.TokenReturn;
import com.stb.blog.models.User;
import com.stb.blog.models.UserReturn;
import com.stb.blog.services.JwtService;
import com.stb.blog.services.UserService;
import jakarta.annotation.security.DeclareRoles;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


@RestController
@DeclareRoles({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER","ROLE_USER"})
@RequestMapping("/api/blog/users")
public class UserController{
    @Autowired
    UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;



    @GetMapping("/exists/{username}")
    public ResponseEntity<?> checkUsernameExists(@PathVariable String username){
        var user = userService.findUserByUsername(username);
        if(user != null) return new ResponseEntity<>("Username Exists",HttpStatus.CONFLICT);
        return new ResponseEntity<>("Username available",HttpStatus.OK);
    }

    @PostMapping("/all-users")
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<String>> getAllUsernames(){
        var usernames = userService.getAllUsernames();
        return new ResponseEntity<>(usernames,HttpStatus.OK);
    }

    @GetMapping("/details/{username}")
    public ResponseEntity<UserReturn> getUserDetailsFromUserName(@PathVariable String username){
        var user = userService.findUserByUsername(username);
        if(user != null) return new ResponseEntity<>(user.getUserReturn(),HttpStatus.OK);
        return  new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginReturn> refreshTokens(@RequestHeader String token){
        if(!jwtService.validateRefreshToken(token))return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        String username = jwtService.getUsernameFromRefreshToken(token);
        User user = userService.findUserByUsername(username);
        TokenReturn newAccessToken;
        TokenReturn newRefreshToken;
        try{
            newAccessToken=jwtService.generateAccessToken(user);
            newRefreshToken = jwtService.generateRefreshToken(user);
        }catch(InvalidUserCredentialsException e){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.set("token",newRefreshToken.getToken());
        resHeaders.set("expires",newRefreshToken.getExpires().toString());
        return ResponseEntity.ok().headers(resHeaders).body(new LoginReturn(newAccessToken,user.getUserReturn()));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestBody Map<String,String> payload){
        if(!payload.containsKey("token")) return new ResponseEntity<>(false,HttpStatus.BAD_REQUEST);
        String token = payload.get("token");
        boolean valid = jwtService.validateAccessToken(token);
        return new ResponseEntity<>(valid,HttpStatus.OK);
    }

    @PostMapping("/revalidate-token")
    public ResponseEntity<TokenReturn> revalidateToken(@RequestBody Map<String,String> payload){
        if(!payload.containsKey("token"))return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        String username = jwtService.getUsernameFromAccessToken(payload.get("token"));
        User user = userService.findUserByUsername(username);
        TokenReturn newToken ;
        try {
            newToken=jwtService.generateAccessToken(user);
        }catch (Exception e){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(newToken,HttpStatus.OK);
    }
    @PostMapping("/has-any-auth")
    public ResponseEntity<UserReturn> hasAnyAuth(@RequestBody AuthCheck payload){
        if(payload.token() == null || payload.roles()==null)return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        String token = payload.token();
        List<String> roles = payload.roles();
        var username = jwtService.getUsernameFromAccessToken(token);
        var user = userService.findUserByUsername(username);
        for(var role : roles){
            if(user.hasRole(role)){
                return new ResponseEntity<>(user.getUserReturn(),HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/has-all-auth")
    public ResponseEntity<UserReturn> hasAllAuth(@RequestBody AuthCheck payload){
        if(payload.token() == null || payload.roles()==null)return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        String token = payload.token();
        List<String> roles = payload.roles();
        var username = jwtService.getUsernameFromAccessToken(token);
        var user = userService.findUserByUsername(username);
        for(var role : roles){
            if(!user.hasRole(role)){
                return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
            }
        }
        return new ResponseEntity<>(user.getUserReturn(),HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginReturn> login(@RequestBody Map<String,Object> payload){
        if(!payload.containsKey("username") || !payload.containsKey("password")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        String username = payload.get("username").toString();
        String password = payload.get("password").toString();

        var user = userService.findUserByUsernameAndPassword(username,password);
        if(user == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        TokenReturn accessToken;
        TokenReturn refreshTokenReturn;
        try{
            accessToken=jwtService.generateAccessToken(user);
            refreshTokenReturn= jwtService.generateRefreshToken(user);
        } catch(InvalidUserCredentialsException e){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        String refreshToken = refreshTokenReturn.getToken();
        updateUserLastAccess(user);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.set("token",refreshToken);
        resHeaders.set("tokenExpires",refreshTokenReturn.getExpires().toString());
        return ResponseEntity.ok().headers(resHeaders).body(new LoginReturn(accessToken,user.getUserReturn()) );
    }

    @PostMapping("/login-with-token")
    public ResponseEntity<?> loginWithToken(@RequestBody Map<String,String> payload){
        if(!payload.containsKey("jwt")) return  new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        String token = payload.get("jwt");
        String username = jwtService.getUsernameFromRefreshToken(token);
        User user = userService.findUserByUsername(username);
        TokenReturn accessToken,refreshToken;
        try{
            accessToken = jwtService.generateAccessToken(user);
            refreshToken = jwtService.generateRefreshToken(user);
        }catch (InvalidUserCredentialsException e){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        updateUserLastAccess(user);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.set("token",refreshToken.getToken());
        resHeaders.set("tokenExpires",refreshToken.getExpires().toString());
        return ResponseEntity.ok().headers(resHeaders).body(new LoginReturn(accessToken,user.getUserReturn()));
    }

    //TODO UPDATE
    @PostMapping("/register")
    public ResponseEntity<LoginReturn> registerUser(@RequestBody Map<String,Object> payload){
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
        user.setRoles(new HashSet<>());
        user.addRole("ROLE_USER");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        TokenReturn jwtString;
        try{
            jwtString=jwtService.generateAccessToken(user);
        }catch(InvalidUserCredentialsException e){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        userService.saveNewUser(user);
        return new ResponseEntity<>(new LoginReturn(jwtString,user.getUserReturn()),HttpStatus.CREATED);
    }


    @PutMapping("/add-auth/admin")
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
    //@RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
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
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
//    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
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
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
//    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
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
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
//    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
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
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
//    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
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
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
//    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN"})
    public ResponseEntity<?> removeUserRoleModerator(@RequestBody Map<String,Object> payload){
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

    @PutMapping("/ban-user/{username}")
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserReturn> banUser(@PathVariable String username){
        User userToBan = userService.findUserByUsername(username);
        if(userToBan == null) return  new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        userToBan.ban();
        var saved = userService.saveNewUser(userToBan);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/unban-user/{username}")
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserReturn> unbanUser(@PathVariable String username){
        User userToUnban = userService.findUserByUsername(username);
        if(userToUnban == null) return  new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        userToUnban.unban();
        var saved = userService.saveNewUser(userToUnban);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/disable-user/{username}")
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserReturn> disableUser(@PathVariable String username){
        User user = userService.findUserByUsername(username);
        if(user == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        user.disable();
        var saved = userService.saveNewUser(user);
        if(saved == null)return  new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @PutMapping("/enable-user/{username}")
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserReturn> enableUser(@PathVariable String username){
        User user = userService.findUserByUsername(username);
        if(user == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        user.enable();
        var saved = userService.saveNewUser(user);
        if(saved == null)return  new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(saved.getUserReturn(),HttpStatus.OK);
    }

    @DeleteMapping("delete-user/{username}")
    @PreAuthorize("hasRole('ROLE_OWNER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserReturn> deleteUser(@PathVariable String username){
        User user = userService.findUserByUsername(username);
        if(user == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        var deleted = userService.deleteUser(user);
        if(deleted == null)return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(user.getUserReturn(), HttpStatus.OK);
    }




    private User updateUserLastAccess(User user){
        Date dateNow = new Date();
        user.setLastAccess(dateNow);
        var saved = userService.saveNewUser(user);
        return saved;
    }

}

record LoginReturn(TokenReturn token, UserReturn details){}
record AuthCheck(String token,List<String> roles){}

package com.stb.blog.models;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Data
@Document("Users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User implements UserDetails {
    @Id
    private ObjectId id;
    private String userId;
    private String username;
    private String password;
    private Date createdOn;
    private Date lastAccess;
    private Set<Role> roles;
    private boolean isBanned = false;
    private boolean isDisabled = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        for(var role : roles) authorityList.add(new SimpleGrantedAuthority(role.getName()));
        return authorityList;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean addRole(String role){
        return roles.add(new Role(role));
    }

    public boolean removeRole(String role) {
        for(var savedRoll : roles){
            if(savedRoll.isEqualToString(role)){
                var res = roles.remove(savedRoll);
                return res;
            }
        }
        return false;
    }

    public boolean hasRole(String role){
        for(var savedRole : roles){
            if(savedRole.getName().equals(role))return true;
        }
        return false;
    }

    public UserReturn getUserReturn(){
        Set<String> roleList=new HashSet<>();
        for(var role : roles) roleList.add(role.getName());
        return new UserReturn(userId,username,roleList,createdOn,isBanned,isDisabled);
    }

    public void ban(){
        this.isBanned=true;
    }

    public void unban(){
        isBanned = false;
    }

    public void disable(){
        isDisabled = true;
    }

    public void enable(){
        isDisabled = false;
    }

    public String getHighestRole(){
        if(hasRole("ROLE_OWNER"))return "Owner";
        if(hasRole("ROLE_ADMIN"))return "Admin";
        if(hasRole("ROLE_MODERATOR"))return "Moderator";
        if(hasRole("ROLE_WRITER")) return "Writer";
        return "User";

    }
}

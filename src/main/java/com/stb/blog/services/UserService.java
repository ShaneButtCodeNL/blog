package com.stb.blog.services;

import com.stb.blog.models.User;
import com.stb.blog.repositories.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    //@Autowired
    PasswordEncoder passwordEncoder;

    public User saveNewUser(User user){
        var saved = userRepository.save(user);
        return saved;
    }

    public User findUserByUsernameAndPassword(String username,String password){
        var user = userRepository.findUserByUsername(username);
        if(user == null) return null;
        var decryptMatch= new BCryptPasswordEncoder().matches(password,user.getPassword());
        if(user!=null && decryptMatch) return user;
        return null;
    }

    public User findUserByUsername(String username){
        var user= userRepository.findUserByUsername(username);
        return user;
    }

    public User findUserById(ObjectId id){
        var user = userRepository.findUserById(id);
        return user;
    }

    public User findUserByUserId(String userId){
        var user = userRepository.findUserByUserId(userId);
        return user;
    }

    public User deleteUser(User user){
        var del = mongoTemplate.remove(user).getDeletedCount();
        if(del > 0){
            return user;
        }
        return null;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = userRepository.findUserByUsername(username);
        if(user == null) throw new UsernameNotFoundException("User [ " + username + " ] not found");

        return user;
    }

    public List<String> getAllUsernames(){
        ArrayList<String> usernames = new ArrayList<>();
        var userList = userRepository.findAll();
        for( var user : userList){
            usernames.add(user.getUsername());
        }
        return usernames;
    }
}

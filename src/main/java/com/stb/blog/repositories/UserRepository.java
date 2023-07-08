package com.stb.blog.repositories;

import com.stb.blog.models.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User,String> {

    User findUserByUsername(String username);


    User findUserById(ObjectId id);

    User findUserByUserId(String userId);
}

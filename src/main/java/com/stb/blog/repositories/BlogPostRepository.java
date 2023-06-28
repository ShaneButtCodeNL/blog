package com.stb.blog.repositories;

import com.stb.blog.models.BlogPost;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BlogPostRepository extends MongoRepository<BlogPost,String> {

    BlogPost getBlogPostById(ObjectId id);

    BlogPost getBlogPostByBlogId(String blogId);
}

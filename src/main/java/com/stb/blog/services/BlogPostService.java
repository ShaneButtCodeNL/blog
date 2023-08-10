package com.stb.blog.services;

import com.stb.blog.models.BlogPost;
import com.stb.blog.models.BlogPostComment;
import com.stb.blog.repositories.BlogPostRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BlogPostService {
    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<BlogPost> getAllBlogPosts(){
        List<BlogPost> posts=blogPostRepository.findAll();
        if(posts==null)posts = new ArrayList<BlogPost>();
        return posts;
    }

    public BlogPost getBlogPostWithId(ObjectId id){
        BlogPost blogPost = blogPostRepository.getBlogPostById(id);
        return blogPost;
    }

    public BlogPost getBlogPostWithBlogId(String blogId){
        BlogPost blogPost = blogPostRepository.getBlogPostByBlogId(blogId);
        return blogPost;
    }

    public List<BlogPost> getAllBlogPostsWithTitleContaining(String titleSnip){
        List<BlogPost> posts=blogPostRepository.findAll();
        if(posts==null)return new ArrayList<BlogPost>();
        posts.removeIf(post->!post.getTitle().toLowerCase().contains((CharSequence) titleSnip.toLowerCase()));
        return posts;
    }

    public List<BlogPost> getAllBlogPostsMadeAfter(Date createdOn){
        Query query = new Query();
        query.addCriteria(Criteria.where("createdOn").lte(createdOn));
        List<BlogPost> posts = mongoTemplate.find(query,BlogPost.class);
        if(posts==null)posts=new ArrayList<BlogPost>();
        return posts;
    }

    public BlogPost addNewBlogPost(BlogPost blogPost){
        BlogPost saved=mongoTemplate.save(blogPost);
        return saved;
    }

    public BlogPost updateBlogPost(BlogPost blogPost){
        BlogPost saved = mongoTemplate.save(blogPost);
        return saved;
    }

    public BlogPost flagAsDeleteBlogPost(BlogPost blogPost){
        blogPost.setDeleted(true);
        var saved =mongoTemplate.save(blogPost);
        return saved;
    }

    public BlogPostComment flagAsDeleteBlogPostComment(BlogPostComment blogPostCommentToBeDeleted){
        var blogPost = this.getBlogPostWithBlogId(blogPostCommentToBeDeleted.getBlogId());
        var blogPostOpt = blogPost.findCommentById(blogPostCommentToBeDeleted.getCommentId());
        if(blogPostOpt.isEmpty())return null;
        var blogPostComment = blogPostOpt.get();
        blogPostComment.setDeleted(true);
        mongoTemplate.save(blogPost);
        return blogPostComment;
    }

    public BlogPost restoreBlogPost(BlogPost blogPost){
        blogPost.setDeleted(false);
        return mongoTemplate.save(blogPost);
    }

    public BlogPostComment restoreBlogPostComment(BlogPostComment blogPostComment){
        BlogPost blogPost = getBlogPostWithBlogId(blogPostComment.getBlogId());
        var blogPostOpt = blogPost.findCommentById(blogPostComment.getCommentId());
        if(blogPostOpt.isEmpty())return null;
        blogPostOpt.get().setDeleted(false);
        mongoTemplate.save(blogPost);
        return blogPostOpt.get();
    }

    public BlogPost removeBlogPostFromDB(BlogPost blogPost){
        var removed = mongoTemplate.remove(blogPost).getDeletedCount();
        return removed>0?blogPost:null;
    }

    public BlogPostComment removeBlogPostCommentFromDB(BlogPostComment blogPostComment){
        // Post comment belongs to
        BlogPost blogPost = getBlogPostWithBlogId(blogPostComment.getBlogId());

        // Comment in blog Post
        var foundCommentOptional = blogPost.findCommentById(blogPostComment.getCommentId());
        if(foundCommentOptional.isEmpty())return null;
        var foundComment = foundCommentOptional.get();

        // If comment doesn't have a parent comment id it is comment to post
        if(foundComment.getParentCommentId() == null){
            boolean deleted=blogPost.getComments().removeIf(bpc -> bpc.getCommentId().equals(blogPostComment.getCommentId()));
            mongoTemplate.save(blogPost);
            return deleted?blogPostComment:null;
        }

        //Else it is a reply to another comment
        var parentOptional = blogPost.findCommentById(foundComment.getParentCommentId());
        if(parentOptional.isEmpty())return null;
        var parent = parentOptional.get();
        boolean deleted = parent.getReplies().removeIf(bpc -> bpc.getCommentId().equals(foundComment.getCommentId()));
        mongoTemplate.save(blogPost);
        return deleted?blogPostComment:null;
    }
}

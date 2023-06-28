package com.stb.blog.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Document("Posts")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BlogPost {
    private ObjectId id;
    private String blogId;
    private String body;
    private Date createdOn;
    private Date lastUpdated;
    private String author;
    private String title;
    private List<BlogPostComment> comments;
    private Set<String> likes;
    private boolean deleted;

    public BlogPostReturn getBlogPostReturn(){
        List<BlogPostCommentReturn> returns=new ArrayList<>();
        for(var comment : comments)returns.add(comment.toBlogPostCommentReturn());
        return new BlogPostReturn(
                blogId,
                title,
                body,
                author,
                createdOn,
                lastUpdated,
                returns,
                likes,
                deleted,
                this.countTopLevelReplies(),
                this.countTotalReplies()
        );
    }

    public int countTopLevelReplies(){
        var count = comments.size();
        return count;
    }

    public int countTotalReplies(){
        var count = comments.size();
        for(var comment:comments)count+=comment.countReplies();
        return count;
    }

    public Optional<BlogPostComment> findCommentById(String searchId){
        for(var comment:comments){
            var foundComment = comment.findComment(searchId);
            if(foundComment.isPresent())return foundComment;
        }
        return Optional.empty();
    }

    public void addComment(BlogPostComment blogPostComment){
        comments.add(blogPostComment);
    }
}

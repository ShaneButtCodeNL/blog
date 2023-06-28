package com.stb.blog.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostComment {
    private ObjectId id;
    private String title;
    private String author;
    private String body;
    private Date createdOn;
    private Date lastUpdated;
    private boolean deleted=false;
    private List<BlogPostComment> replies;
    private String blogId;
    private String parentCommentId;
    private String commentId;
    private Set<String> likes;

    public BlogPostCommentReturn toBlogPostCommentReturn(){
        List<BlogPostCommentReturn> commentReturns=new ArrayList<>();
        for(var bpc : replies) commentReturns.add(bpc.toBlogPostCommentReturn());
        return new BlogPostCommentReturn(
                commentId,
                blogId,
                parentCommentId,
                likes,
                title,
                author,
                body,
                createdOn,
                lastUpdated,
                deleted,
                commentReturns,
                this.replies.size(),
                this.countReplies()
        );
    }

    public int countReplies(){
        var count= replies.size();
        for(var reply : replies){
            count+=reply.countReplies();
        }
        return count;
    }

    public Optional<BlogPostComment> findComment(String searchId){
        if(searchId==null) return Optional.empty();
        if(commentId.equals(searchId))return Optional.of(this);
        for(var reply:replies){
            var foundComment = reply.findComment(searchId);
            if(foundComment.isPresent())return foundComment;
        }
        return Optional.empty();
    }

    public void addComment(BlogPostComment comment){
        replies.add(comment);
    }
}

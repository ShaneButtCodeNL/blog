package com.stb.blog.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostCommentReturn {
    private String commentId;
    private String blogId;
    private String parentCommentId;
    private Set<String> likes;
    private String title;
    private String author;
    private String body;
    private Date createdOn;
    private Date lastUpdated;
    private boolean deleted=false;
    private List<BlogPostCommentReturn> replies;
    private int topLevelCommentCount=0;
    private int totalCommentCount=0;
}

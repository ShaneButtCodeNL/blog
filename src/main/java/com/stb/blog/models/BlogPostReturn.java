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
public class BlogPostReturn {
    private String blogId;
    private String title;
    private String body;
    private String author;
    private Date createdOn;
    private Date lastUpdated;
    private List<BlogPostCommentReturn> comments;
    private Set<String> likes;
    private boolean deleted=false;
    private int topLevelCommentCount=0;
    private int totalCommentCount=0;
}

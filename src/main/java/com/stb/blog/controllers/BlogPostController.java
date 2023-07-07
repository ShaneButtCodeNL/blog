package com.stb.blog.controllers;

import com.stb.blog.exceptions.JwtTokenException;
import com.stb.blog.models.*;
import com.stb.blog.services.BlogPostService;
import com.stb.blog.services.JwtService;
import com.stb.blog.services.UserService;
import jakarta.annotation.security.RolesAllowed;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/blog/posts")
public class BlogPostController {

    @Autowired
    BlogPostService blogPostService;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserService userService;

    // Get Methods

    @GetMapping("/")
    public ResponseEntity<List<BlogPostReturn>> getAllBlogPosts(){
        return new ResponseEntity<>(blogPostService.getAllBlogPosts().stream().map(blogPost -> blogPost.getBlogPostReturn()).toList(), HttpStatus.OK);
    }

    @GetMapping("/post/{blogId}")
    public ResponseEntity<BlogPostReturn> getBlogPostById(@PathVariable String blogId){
        var blogPost = blogPostService.getBlogPostWithBlogId(blogId);
        if(blogPost==null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(blogPost.getBlogPostReturn(),HttpStatus.OK);
    }

    @GetMapping("/search/title/{titleSnip}")
    public ResponseEntity<List<BlogPostReturn>> getBlogPostsWithTitle(@PathVariable String titleSnip){
        return new ResponseEntity<>(blogPostService.getAllBlogPostsWithTitleContaining(titleSnip).stream().map(blogPost -> blogPost.getBlogPostReturn()).toList(),HttpStatus.OK);
    }

    @GetMapping("/search/date/after/{dateString}")
    public ResponseEntity<List<BlogPostReturn>> getBlogPostsCreatedAfterDate(@PathVariable String dateString) throws ParseException {
        Date date= new SimpleDateFormat("dd-mm-yyyy").parse(dateString);
        return new ResponseEntity<>(blogPostService.getAllBlogPostsMadeAfter(date).stream().map(blogPost -> blogPost.getBlogPostReturn()).toList(),HttpStatus.OK);
    }

    // Post/Put Methods


    @PostMapping("/")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER"})
    public ResponseEntity<BlogPostReturn> makeNewBlogPost(@RequestBody Map<String,Object> payload , @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        Date dateNow=new Date();
        if(!payload.containsKey("title") || !payload.containsKey("body") )return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        String author = jwtService.getUsernameFromToken(jwtString);
        String title = payload.get("title").toString();
        String body = payload.get("body").toString();
        BlogPost bp = new BlogPost();
        bp.setLastUpdated(dateNow);
        bp.setBody(body);
        bp.setAuthor(author);
        bp.setTitle(title);
        bp.setCreatedOn(dateNow);
        bp.setComments(new ArrayList<BlogPostComment>());
        bp.setLikes(new HashSet<String>());
        bp.setBlogId(new ObjectId().toHexString());
        BlogPost blogPost = blogPostService.addNewBlogPost(bp);
        if(blogPost == null)return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(blogPost.getBlogPostReturn(),HttpStatus.CREATED);
    }

    @PutMapping("/")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER"})
    public ResponseEntity<BlogPostReturn> updateExistingBlogPost(@RequestBody Map<String,String> payload , @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        Date dateNow = new Date();
        if(!payload.containsKey("blogId")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        // get user
        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }

        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        boolean hasChanged=false;

        //Check role
        if( !requestUser.hasRole("ROLE_ADMIN") && !requestUser.hasRole("ROLE_OWNER") &&  !blogPost.getAuthor().equals(requestUser.getUsername()) )
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);

        //Update body
        if(payload.containsKey("body")){
            hasChanged=true;
            blogPost.setBody(payload.get("body").toString());
        }
        // Update Author
        if(payload.containsKey("author")){
            hasChanged=true;
            blogPost.setAuthor(payload.get("author").toString());
        }
        // Update Title
        if(payload.containsKey("title")){
            hasChanged=true;
            blogPost.setTitle(payload.get("title").toString());
        }
        // If anything was updated change last update date ,save to database and return new blog post
        if(hasChanged){
            blogPost.setLastUpdated(dateNow);
            var saved = blogPostService.updateBlogPost(blogPost);
            return new ResponseEntity<>(saved.getBlogPostReturn(),HttpStatus.CREATED);
        }
        // If no update, Don't update database return the blog post
        return new ResponseEntity<>(blogPost.getBlogPostReturn(),HttpStatus.OK);
    }

    @PostMapping("/comment")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_USER"})
    public ResponseEntity<BlogPostReturn> addCommentToBlogPost(@RequestBody Map<String,Object> payload , @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        Date dateNow=new Date();
        if(!payload.containsKey("blogId") || !payload.containsKey("body")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        BlogPostComment blogPostComment = new BlogPostComment();
        // Set the blog this comment belongs to
        blogPostComment.setBlogId(payload.get("blogId").toString());
        // If this is a reply to another comment set it as parent else null
        blogPostComment.setParentCommentId(
                payload.containsKey("parentCommentId")?
                    payload.get("parentCommentId").toString(): null
        );
        // Set body
        blogPostComment.setBody(payload.get("body").toString());
        // Set creation dates
        blogPostComment.setCreatedOn(dateNow);
        blogPostComment.setLastUpdated(dateNow);
        // Make sure comment is not deleted
        blogPostComment.setDeleted(false);
        // Set replies to empty list
        blogPostComment.setReplies(new ArrayList<BlogPostComment>());

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }
        // Set author
        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null){
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        blogPostComment.setAuthor(requestUser.getUsername());
        // Set Title or give default if no title given
        blogPostComment.setTitle(
                payload.containsKey("title")?
                        payload.get("title").toString():""
        );
        // Set List for likes
        blogPostComment.setLikes(new HashSet<String>());
        // Set an id to use to identify without reveling id
        blogPostComment.setCommentId(new ObjectId().toHexString());



        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(blogPostComment.getBlogId());
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        if(blogPostComment.getParentCommentId()!=null){
            var parentBlogPost = blogPost.findCommentById(blogPostComment.getParentCommentId());
            if(parentBlogPost.isEmpty()) return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
            parentBlogPost.get().addComment(blogPostComment);
        }
        else{
            blogPost.addComment(blogPostComment);
        }
        var savedBlogPost = blogPostService.updateBlogPost(blogPost);
        return new ResponseEntity<>(savedBlogPost.getBlogPostReturn(),HttpStatus.CREATED);
    }


    @PutMapping("/comment")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_USER"})
    public ResponseEntity<?> updateCommentInBlogPost(@RequestBody Map<String,Object> payload , @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        Date dateNow = new Date();
        //Check for blogId and commentId
        if(!payload.containsKey("blogId") || !payload.containsKey("commentId") ){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        boolean hasChanged = false;

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.UNAUTHORIZED);
        }

        // Find BlogPost
        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        if(blogPost==null)return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        // Find Comment
        var blogPostCommentOptional = blogPost.findCommentById(payload.get("commentId").toString());
        if(blogPostCommentOptional.isEmpty()) return new ResponseEntity<>("Comment Not Found.",HttpStatus.NOT_FOUND);
        var blogPostComment = blogPostCommentOptional.get();

        //Check role
        String requestUsername = jwtService.getUsernameFromToken(jwtString);
        User requestUser = userService.findUserByUsername(requestUsername);
        if(requestUser == null)return new ResponseEntity<>("User not found.",HttpStatus.NOT_FOUND);

        if( requestUser.hasRole("ROLE_OWNER") || requestUser.hasRole("ROLE_ADMIN") || requestUser.getUsername().equals(blogPostComment.getAuthor())){
            // Update title if given a new one
            if(payload.containsKey("title")){
                hasChanged=true;
                blogPostComment.setTitle(payload.get("title").toString());
            }

            // Update body if given and not empty
            if(payload.containsKey("body")) {
                String body = payload.get("body").toString();
                if(!body.isEmpty()){
                    hasChanged = true;
                    blogPostComment.setBody(body);
                }
            }

            // If Updated save to database and return new BlogPost
            if(hasChanged){
                blogPostComment.setLastUpdated(dateNow);
                var saved = blogPostService.updateBlogPost(blogPost);
                return new ResponseEntity<>(saved,HttpStatus.CREATED);
            }
            // Return BlogPost
            return new ResponseEntity<>(blogPost.getBlogPostReturn(),HttpStatus.OK);
        }
        return new ResponseEntity<>("User Not Authorized",HttpStatus.UNAUTHORIZED);
    }

    @PutMapping("/post/like/{blogId}")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER","ROLE_USER"})
    public ResponseEntity<String> likeBlogPost(@PathVariable String blogId, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(blogId);
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null)  return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        String userId = requestUser.getUserId();

        if(blogPost.getLikes().contains(userId)){
            blogPost.getLikes().remove(userId);
            blogPostService.updateBlogPost(blogPost);
            return new ResponseEntity<>("UNLIKED",HttpStatus.OK);
        }
        blogPost.getLikes().add(userId);
        blogPostService.updateBlogPost(blogPost);
        return new ResponseEntity<>("LIKED",HttpStatus.OK);
    }

    @PutMapping("/comment/like/{blogId}/{commentId}")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER","ROLE_USER"})
    public ResponseEntity<String> likeBlogPostComment(
            @PathVariable String blogId,
            @PathVariable String commentId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken
        ){

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null)  return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        String userId = requestUser.getUserId();

        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(blogId);
        if(blogPost==null)return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        var blogPostComment = blogPost.findCommentById(commentId);
        if(blogPostComment.isEmpty())return new ResponseEntity<>("Comment Not Found",HttpStatus.NOT_FOUND);
        if(blogPostComment.get().getLikes().contains(userId)){
            blogPostComment.get().getLikes().remove(userId);
            blogPostService.updateBlogPost(blogPost);
            return new ResponseEntity<>("UNLIKED",HttpStatus.OK);
        }
        blogPostComment.get().getLikes().add(userId);
        blogPostService.updateBlogPost(blogPost);
        return new ResponseEntity<>("LIKED",HttpStatus.OK);
    }

    @PutMapping("/delete/post")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER"})
    public ResponseEntity<BlogPostReturn> flagPostAsDeleted(@RequestBody Map<String,Object> payload,@RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        if(!payload.containsKey("blogId")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null)  return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);


        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        // Can be deleted by owner or writer of post
        if(
                !requestUser.hasRole("ROLE_OWNER") &&
                        !requestUser.hasRole("ROLE_ADMIN") &&
                        !blogPost.getAuthor().equals(requestUser.getUsername())
        )
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        BlogPost deleted = blogPostService.flagAsDeleteBlogPost(blogPost);
        if(deleted == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(deleted.getBlogPostReturn(),HttpStatus.OK);
    }

    @PutMapping("/restore/blog")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER"})
    public ResponseEntity<BlogPostReturn> restoreDeletedBlogPost(@RequestBody Map<String,Object> payload, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        if(!payload.containsKey("blogId")) return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);

        String jwtString;
        try{
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        if(!requestUser.hasRole("ROLE_OWNER") && !requestUser.hasRole("ROLE_ADMIN") && !requestUser.getUsername().equals(blogPost.getAuthor()))
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(blogPostService.restoreBlogPost(blogPost).getBlogPostReturn(),HttpStatus.OK);
    }

    @PutMapping("/delete/comment")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER"})
    public ResponseEntity<BlogPostCommentReturn> flagCommentAsDeleted(@RequestBody Map<String,Object> payload,@RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        if(!payload.containsKey("blogId") || !payload.containsKey("commentId")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null)  return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        var blogPostCommentOptional = blogPost.findCommentById(payload.get("commentId").toString());
        if(blogPostCommentOptional.isEmpty()){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }

        var blogPostComment = blogPostCommentOptional.get();

        // Can be deleted by owner,admin,writer of comment or writer of post
        if(
                !requestUser.hasRole("ROLE_OWNER") &&
                        !requestUser.hasRole("ROLE_ADMIN") &&
                        !blogPostComment.getAuthor().equals(requestUser.getUsername()) &&
                        !blogPost.getAuthor().equals(requestUser.getUsername())
        )
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);

        BlogPostComment deleted = blogPostService.flagAsDeleteBlogPostComment(blogPostComment);
        if(deleted == null){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(deleted.toBlogPostCommentReturn(),HttpStatus.OK);
    }


    @PutMapping("/restore/comment")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER","ROLE_USER"})
    public ResponseEntity<BlogPostCommentReturn> restoreBlogPostComment(@RequestBody Map<String,Object> payload,@RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        if(!payload.containsKey("blogId") || !payload.containsKey("commentId")) return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);

        String jwtString;
        try{
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        var blogPostCommentOptional  = blogPost.findCommentById(payload.get("commentId").toString());
        if(blogPostCommentOptional.isEmpty()) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        BlogPostComment blogPostComment = blogPostCommentOptional.get();

        if(!requestUser.hasRole("ROLE_OWNER") && !requestUser.hasRole("ROLE_ADMIN") && !blogPostComment.getAuthor().equals(requestUser.getUsername()))
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(blogPostService.restoreBlogPostComment(blogPostComment).toBlogPostCommentReturn(),HttpStatus.OK);
    }
    //Delete Requests


    @DeleteMapping("/blog")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER"})
    public ResponseEntity<BlogPostReturn> deleteBlogPost(@RequestBody Map<String,Object> payload, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        if(!payload.containsKey("blogId")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null)  return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);


        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        // Can be deleted by owner or writer of post
        if(
                !requestUser.hasRole("ROLE_OWNER") &&
                !requestUser.hasRole("ROLE_ADMIN") &&
                !blogPost.getAuthor().equals(requestUser.getUsername())
        )
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        BlogPost deleted = blogPostService.removeBlogPostFromDB(blogPost);
        if(deleted == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(deleted.getBlogPostReturn(),HttpStatus.OK);
    }


    @DeleteMapping("/comment")
    @RolesAllowed({"ROLE_OWNER","ROLE_ADMIN","ROLE_WRITER","ROLE_USER"})
    public ResponseEntity<BlogPostCommentReturn> deleteCommentFromBlogPost(@RequestBody Map<String,Object> payload, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken){
        if(!payload.containsKey("blogId") || !payload.containsKey("commentId")){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }

        //Check if token is still valid
        String jwtString ;
        try {
            jwtString=jwtService.getJWTFromBearerToken(bearerToken);
        }catch (JwtTokenException e){
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);
        }

        User requestUser = userService.findUserByUsername(jwtService.getUsernameFromToken(jwtString));
        if(requestUser == null)  return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        BlogPost blogPost = blogPostService.getBlogPostWithBlogId(payload.get("blogId").toString());
        if(blogPost == null) return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);

        var blogPostCommentOptional = blogPost.findCommentById(payload.get("commentId").toString());
        if(blogPostCommentOptional.isEmpty()){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }

        var blogPostComment = blogPostCommentOptional.get();

        // Can be deleted by owner,admin,writer of comment or writer of post
        if(
                !requestUser.hasRole("ROLE_OWNER") &&
                !requestUser.hasRole("ROLE_ADMIN") &&
                !blogPostComment.getAuthor().equals(requestUser.getUsername()) &&
                !blogPost.getAuthor().equals(requestUser.getUsername())
        )
            return new ResponseEntity<>(null,HttpStatus.UNAUTHORIZED);

        BlogPostComment deleted = blogPostService.removeBlogPostCommentFromDB(blogPostComment);
        if(deleted == null){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(deleted.toBlogPostCommentReturn(),HttpStatus.OK);
    }
}

package com.stb.blog.configs;

import com.stb.blog.services.JwtService;
import com.stb.blog.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final String BEARER="Bearer ";
    @Autowired
    JwtService jwtService;
    @Autowired
    UserService userService;



    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Check for bearer token in header
        final String requestTokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(requestTokenHeader == null || requestTokenHeader.isEmpty() || !requestTokenHeader.startsWith(BEARER)){
            filterChain.doFilter(request,response);
            return;
        }

        // Extract and validate token
        final String token = requestTokenHeader.substring(BEARER.length());
        if(!jwtService.validateAccessToken(token)){
            filterChain.doFilter(request,response);
            return;
        }

        // Get user identity
        UserDetails userDetails = userService.loadUserByUsername(jwtService.getUsernameFromAccessToken(token));

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails==null? List.of():userDetails.getAuthorities());
        authenticationToken.setDetails( new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request,response);
    }
}

package com.trecapps.users.controllers;

import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.TcUser;
import com.trecapps.users.models.UserPost;
import com.trecapps.users.services.UserService;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Users")
public class UserController {

    UserService userService;

    String url;

    public UserController(@Value("${tenant.url}")String url, @Autowired
            UserService userService)
    {
        this.userService = userService;
        this.url = url;
    }


    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/createUser")
    public ResponseEntity<String> createNewUser(RequestEntity<UserPost> post)
    {
        logger.info("Creating New User!");
        UserPost postBody = post.getBody();
        postBody.setUserPrincipalName(postBody.getMailNickname() + "@" + url);

        logger.info("Creating logger with User Principal name {}", postBody.getUserPrincipalName());

        // To-Do: Add Validation to the User Post

        // End To-Do
        return userService.createUser(postBody, true);

    }


    @PutMapping("/UserUpdate")
    public ResponseEntity<String> updateUser(RequestEntity<TcUser> post, @AuthenticationPrincipal OidcUser user)
    {
        // To-Do: Make sure post id and uer id match


        // ENd to-Do:

        return userService.updateTcUser(post.getBody());
    }

    @GetMapping("/Current")
    public ResponseEntity<TcUser> getUser(@AuthenticationPrincipal OidcUser user)
    {
        String id = user.getClaims().get("id").toString();

        return userService.getTcUser(id);
    }

    @PostMapping("/passwordUpdate")
    public ResponseEntity<String> updatePassword(RequestEntity<PasswordChange> post)
    {

        return userService.updatePassword(post.getBody(), post.getHeaders().getFirst("Authorization"));
    }
}

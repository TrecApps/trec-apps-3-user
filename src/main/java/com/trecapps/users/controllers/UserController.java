package com.trecapps.users.controllers;

import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.UserPost;
import com.trecapps.users.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Users")
public class UserController {

    @Autowired
    UserService userService;

    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/createUser")
    public ResponseEntity<String> createNewUser(RequestEntity<UserPost> post)
    {
        logger.info("Creating New User!");
        UserPost postBody = post.getBody();
        postBody.setMailNickname(postBody.getMail().substring(0, postBody.getMail().indexOf('@')));

        // To-Do: Add Validation to the User Post

        // End To-Do
        return userService.createUser(postBody);

    }



    @PostMapping("/passwordUpdate")
    public ResponseEntity<String> updatePassword(RequestEntity<PasswordChange> post)
    {

        return userService.updatePassword(post.getBody(), post.getHeaders().getFirst("Authorization"));
    }
}

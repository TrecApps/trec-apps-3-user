package com.trecapps.users.controllers;

import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.UserPost;
import com.trecapps.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/Users")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/createUser")
    public ResponseEntity<String> createNewUser(RequestEntity<UserPost> post)
    {
        UserPost postBody = post.getBody();
        postBody.setMailNickname(postBody.getMail().substring(0, postBody.getMail().indexOf('@')));

        // To-Do: Add Validation to the User Post

        // End To-Do
        return userService.createUser(postBody);

    }

    @PostMapping("/passwordUpdate")
    public ResponseEntity<String> updatePassword(RequestEntity<PasswordChange> post)
    {
        return userService.updatePassword(post.getBody());
    }
}

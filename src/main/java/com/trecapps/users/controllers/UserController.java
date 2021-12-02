package com.trecapps.users.controllers;

import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.UserPost;
import com.trecapps.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController("/Users")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/createUser")
    public Mono<ResponseEntity<String>> createNewUser(RequestEntity<UserPost> post)
    {
        return Mono.just(post)
                .map((RequestEntity<UserPost> request) -> request.getBody())
                .flatMap((UserPost postBody) -> {
                    postBody.setMailNickname(postBody.getMail().substring(0, postBody.getMail().indexOf('@')));
                    // To-Do: Other validation of the User to be created


                    // End to-Do
                    return userService.createUser(postBody);
                });
    }

    @PostMapping("/passwordUpdate")
    public Mono<ResponseEntity<String>> updatePassword(RequestEntity<PasswordChange> post)
    {
        return Mono.just(post)
                .map((RequestEntity<PasswordChange> request) -> request.getBody())
                .flatMap((PasswordChange postBody) -> userService.updatePassword(postBody));
    }
}

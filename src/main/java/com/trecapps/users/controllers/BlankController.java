package com.trecapps.users.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class BlankController {

    @GetMapping("/")
    Mono<ResponseEntity<String>> blank()
    {
        return Mono.just(new ResponseEntity<>("Works!", HttpStatus.OK));
    }

}

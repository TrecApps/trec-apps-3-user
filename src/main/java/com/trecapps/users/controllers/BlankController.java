package com.trecapps.users.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BlankController {

    @GetMapping("/")
    ResponseEntity<String> blank()
    {
        return new ResponseEntity<>("Works!", HttpStatus.OK);
    }

}

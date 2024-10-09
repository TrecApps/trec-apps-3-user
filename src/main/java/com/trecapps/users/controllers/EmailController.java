package com.trecapps.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.users.models.AuthenticationBody;
import com.trecapps.users.services.TrecEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/Email")
public class EmailController {

    @Autowired
    TrecEmailService emailService;

    @GetMapping
    public Mono<ResponseEntity<Void>> sendVerificationEmail(Authentication authentication)
    {
        return Mono.just((TrecAuthentication)authentication)
                .flatMap((TrecAuthentication auth) -> emailService.sendValidationEmail(auth.getUser())
                        .map((String worked) -> new ResponseEntity<>(!worked.isEmpty() ? HttpStatus.NO_CONTENT : HttpStatus.ACCEPTED)));

    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<Void>> postVerification(@RequestBody String code, Authentication auth)
    {
        return Mono.just(new AuthenticationBody<String>((TrecAuthentication)auth, code))
                .flatMap((AuthenticationBody<String> ab) -> {

                        return emailService.validateEmail(ab.getAuthentication().getAccount(), code)
                                .map((Boolean worked) -> new ResponseEntity<>(worked ? HttpStatus.NO_CONTENT : HttpStatus.BAD_REQUEST));

                });
    }

}

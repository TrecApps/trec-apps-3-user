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

import jakarta.mail.MessagingException;
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
                .flatMap((TrecAuthentication auth) -> {
                    try {
                        return emailService.sendValidationEmail(auth.getAccount())
                                .map((Boolean worked) -> new ResponseEntity<>(worked ? HttpStatus.NO_CONTENT : HttpStatus.ACCEPTED));


                    } catch (JsonProcessingException | MessagingException e) {
                        e.printStackTrace();
                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });

    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<Void>> postVerification(@RequestBody String code, Authentication auth)
    {
        return Mono.just(new AuthenticationBody<String>((TrecAuthentication)auth, code))
                .flatMap((AuthenticationBody<String> ab) -> {
                    try{
                        return emailService.validateEmail(ab.getAuthentication().getAccount(), code)
                                .map((Boolean worked) -> new ResponseEntity<>(worked ? HttpStatus.NO_CONTENT : HttpStatus.BAD_REQUEST));
                    }catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });
    }

}

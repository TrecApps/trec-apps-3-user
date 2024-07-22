package com.trecapps.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.users.models.AuthenticationBody;
import com.trecapps.users.services.TrecSmsService;
import com.twilio.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/Sms")
@Slf4j
public class SmsController {
    @Autowired(required = false)
    TrecSmsService trecSmsService;

    @GetMapping
    public Mono<ResponseEntity> sendVerificationText(Authentication authentication)
    {
        return Mono.just(authentication)
                .map(auth -> (TrecAuthentication)auth)
                .map((TrecAuthentication trecAuth) -> {
                    if(trecSmsService == null || !trecSmsService.hasBeenSetUp())
                        return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
                    try{
                        trecSmsService.sendCode(trecAuth.getUser()).subscribe();
                        return new ResponseEntity(HttpStatus.NO_CONTENT);
                    }catch(ApiException e){
                        log.info("Twilio Exception {} with message {}", e.getCode(), e.getMessage());
                        return new ResponseEntity(HttpStatus.BAD_REQUEST);
                    }
                });

    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity> postVerification(@RequestBody String code,Authentication authentication)
    {
        return Mono.just(new AuthenticationBody<>((TrecAuthentication) authentication, code))
                .flatMap((AuthenticationBody<String> ab) -> {
                    if(trecSmsService == null)
                        return Mono.just(new ResponseEntity(HttpStatus.NOT_IMPLEMENTED));
                    try {
                        return trecSmsService.validatePhone(ab.getAuthentication().getAccount(), ab.getData())
                                .map((Boolean worked) -> new ResponseEntity(worked ? HttpStatus.NO_CONTENT : HttpStatus.BAD_REQUEST));
                    }catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return Mono.just(new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR));
                    }

                });

    }
}

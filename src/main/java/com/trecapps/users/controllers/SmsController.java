package com.trecapps.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.users.services.TrecSmsService;
import com.twilio.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Sms")
@Slf4j
public class SmsController {
    @Autowired(required = false)
    TrecSmsService trecSmsService;

    @GetMapping
    public ResponseEntity sendVerificationText()
    {
        if(trecSmsService == null || !trecSmsService.hasBeenSetUp())
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        try{
            trecSmsService.sendCode(auth.getAccount());
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        } catch (JsonProcessingException e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }catch(ApiException e){
            log.info("Twilio Exception {} with message {}", e.getCode(), e.getMessage());
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity postVerification(@RequestBody String code)
    {
        if(trecSmsService == null)
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        try
        {
            log.info("Attempting to Validate Phone!");
            return new ResponseEntity(trecSmsService.validatePhone(auth.getAccount(), code)
                    ? HttpStatus.NO_CONTENT : HttpStatus.BAD_REQUEST);
        }catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

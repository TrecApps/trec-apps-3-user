package com.trecapps.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.users.services.TrecEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/Email")
public class EmailController {

    @Autowired
    TrecEmailService emailService;

    @GetMapping
    public ResponseEntity sendVerificationEmail()
    {
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();

        try {
            if(emailService.sendValidationEmail(auth.getAccount()))
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            else {
                return new ResponseEntity(HttpStatus.ACCEPTED);
            }
        } catch (JsonProcessingException | MessagingException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity postVerification(@RequestBody String code)
    {
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        try
        {
            return new ResponseEntity(emailService.validateEmail(auth.getAccount(), code)
                    ? HttpStatus.NO_CONTENT : HttpStatus.BAD_REQUEST);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

package com.trecapps.users.controllers;

import com.trecapps.auth.common.models.Session;
import com.trecapps.auth.common.models.SessionList;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.webflux.services.SessionManagerAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/Sessions")
public class SessionController {

    @Autowired
    SessionManagerAsync sessionManager;

    Logger logger = LoggerFactory.getLogger(SessionController.class);

    @GetMapping("/List")
    Mono<ResponseEntity<SessionList>> retrieveSessions(Authentication authentication)
    {
        return Mono.just((TrecAuthentication) authentication)
                .flatMap((TrecAuthentication auth) -> sessionManager.getSessionList(auth.getAccount().getId()))
                .map((List<Session> sessions) -> ResponseEntity.ok(new SessionList(sessions)));
    }

    @GetMapping(value = "/Current", produces = MediaType.TEXT_PLAIN_VALUE)
    Mono<ResponseEntity<String>> retrieveCurrentSession(Authentication authentication)
    {
        return Mono.just((TrecAuthentication) authentication)
                .map(auth -> ResponseEntity.ok(auth.getSessionId()));
    }

    @DeleteMapping(value = "/{sessionId}", produces = MediaType.TEXT_PLAIN_VALUE)
    Mono<ResponseEntity<String>> endSession(@PathVariable("sessionId")String session, Authentication authentication)
    {

        return Mono.just((TrecAuthentication) authentication)
                .flatMap((TrecAuthentication auth) -> sessionManager.removeSession(auth.getAccount().getId(), session))
                .map((Boolean worked) -> {
                    if(worked)
                        return new ResponseEntity<>("Session Deleted!", HttpStatus.OK);
                    else return new ResponseEntity<>("Failed to Remove Session!", HttpStatus.INTERNAL_SERVER_ERROR);
                });


    }
}

package com.trecapps.users.controllers;

import com.trecapps.auth.common.models.SessionListV2;
import com.trecapps.auth.common.models.SessionV2;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.webflux.services.V2SessionManagerAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/Sessions")
public class SessionController {

    @Autowired
    V2SessionManagerAsync sessionManager;

    Logger logger = LoggerFactory.getLogger(SessionController.class);

    @GetMapping("/List")
    Mono<ResponseEntity<SessionListV2>> retrieveSessions(Authentication authentication)
    {
        return Mono.just((TrecAuthentication) authentication)
                .flatMap((TrecAuthentication auth) -> sessionManager.getSessionList(auth.getAccount().getId()))
                .map((List<SessionV2> sessions) -> {
                    SessionListV2 ret = new SessionListV2();
                    ret.setSessions(sessions);
                    ret.prep();
                    return ResponseEntity.ok(ret);
                });
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
                .flatMap((TrecAuthentication auth) -> sessionManager.removeSessionMono(auth.getAccount().getId(), session))
                .thenReturn(ResponseEntity.ok("Removed"));


    }
}

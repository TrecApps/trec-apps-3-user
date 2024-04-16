package com.trecapps.users.controllers;

import com.trecapps.auth.models.SessionList;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.services.core.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Sessions")
public class SessionController {

    @Autowired
    SessionManager sessionManager;

    Logger logger = LoggerFactory.getLogger(SessionController.class);

    @GetMapping("/List")
    ResponseEntity<SessionList> retrieveSessions()
    {
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(new SessionList(sessionManager.getSessionList(auth.getAccount().getId())));
    }

    @GetMapping(value = "/Current", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> retrieveCurrentSession()
    {
        {
            TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();

            logger.info("Auth Session is {}", auth.getSessionId());
            return ResponseEntity.ok(auth.getSessionId());
        }
    }

    @DeleteMapping(value = "/{sessionId}", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> endSession(@PathVariable("sessionId")String session)
    {
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();

        logger.info("Auth Session is {}", auth.getSessionId());

        if(sessionManager.removeSession(auth.getAccount().getId(), session))
            return new ResponseEntity<>("Session Deleted!", HttpStatus.OK);
        else return new ResponseEntity<>("Failed to Remove Session!", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

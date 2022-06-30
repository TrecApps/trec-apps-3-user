package com.trecapps.users.controllers;

import com.trecapps.auth.models.SessionList;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.services.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Sessions")
public class SessionController {

    @Autowired
    SessionManager sessionManager;

    @GetMapping("/List")
    ResponseEntity<SessionList> retrieveSessions()
    {
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(new SessionList(sessionManager.getSessionList(auth.getAccount().getId())));
    }

    @DeleteMapping("/{sessionId}")
    ResponseEntity<String> endSession(@PathVariable("sessionId")String session)
    {
        TrecAuthentication auth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if(sessionManager.removeSession(auth.getAccount().getId(), session))
            return new ResponseEntity<>("Session Deleted!", HttpStatus.OK);
        else return new ResponseEntity<>("Failed to Remove Session!", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

package com.trecapps.users.controllers;

import com.trecapps.auth.services.TrecAccountService;
import com.trecapps.users.models.Login;
import com.trecapps.users.models.LoginToken;
import com.trecapps.users.models.TokenRequest;
import com.trecapps.users.services.StateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/Auth")
public class AuthController {

    @Autowired
    TrecAccountService authService;

    @Autowired
    StateService stateService;

    @Value("${base.url}")
    String baseUrl;

    @Value("${tenant.url}")String url;

    private ResponseEntity<LoginToken> generateResponse(LoginToken token)
    {
        if(token.getAccess_token() == null || token.getAccess_token().length() == 0)
            return new ResponseEntity<LoginToken>(token, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<LoginToken>(token, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginToken> login(@RequestBody Login login)
    {
//        if(!login.getUsername().endsWith(url))
//            login.setUsername(login.getUsername() + '@' + url);
//        return generateResponse(authService.(login).getBody());
        return null;
    }

    @GetMapping("/login")
    public ResponseEntity login()
    {
        // To-Do: Prep means ot managing state parameter
        return new ResponseEntity(stateService.generateState(), HttpStatus.OK);
    }


    @PostMapping("/refresh")
    public ResponseEntity<LoginToken> refreshCode(@RequestBody String refreshToken, @RequestParam("Redirect")String redirect)
    {
        // return generateResponse(authService.getTokenFromRefresh(redirect, refreshToken).getBody());
        return null;
    }


}

package com.trecapps.users.controllers;

import com.trecapps.users.models.Login;
import com.trecapps.users.models.LoginToken;
import com.trecapps.users.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController("/Auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @Value("${base.url}")
    String baseUrl;

    private ResponseEntity<LoginToken> generateResponse(LoginToken token)
    {
        if(token.getAccess_token() == null || token.getAccess_token().length() == 0)
            return new ResponseEntity<LoginToken>(token, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<LoginToken>(token, HttpStatus.OK);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginToken>> login(Login login)
    {
        return authService.getTokenDirectly(login)
                .map((LoginToken token) -> generateResponse(token));
    }

    @GetMapping("/login")
    public Mono<ResponseEntity> login()
    {
        // To-Do: Prep means ot managing state parameter
        return null;
    }

    @GetMapping("/token")
    public Mono<ResponseEntity<LoginToken>> authorizeCode(@RequestParam("code")String code,
                                                          @RequestParam(value = "state", required = false) String state)
    {
        return authService.getTokenFromMSFlow(baseUrl + "/Auth/token", code)
                .map((LoginToken token) -> generateResponse(token));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginToken>> refreshCode(@RequestBody String refreshToken)
    {
        return authService.getTokenFromRefresh(baseUrl + "/Auth/token", refreshToken)
                .map((LoginToken token) -> generateResponse(token));
    }


}

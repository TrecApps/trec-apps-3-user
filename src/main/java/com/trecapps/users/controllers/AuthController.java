package com.trecapps.users.controllers;

import com.trecapps.auth.models.LoginToken;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.JwtTokenService;
import com.trecapps.auth.services.TrecAccountService;
import com.trecapps.users.models.Login;
import com.trecapps.users.models.TokenRequest;
import com.trecapps.users.services.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/Auth")
public class AuthController {

    @Autowired
    TrecAccountService authService;

    @Autowired
    JwtTokenService jwtTokenService;

    @Autowired
    StateService stateService;

    @Value("${base.url}")
    String baseUrl;

    @Value("${tenant.url}")String url;

    Logger logger = LoggerFactory.getLogger(AuthController.class);

    private ResponseEntity<LoginToken> generateResponse(LoginToken token)
    {
        if(token.getAccess_token() == null || token.getAccess_token().length() == 0)
            return new ResponseEntity<LoginToken>(token, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<LoginToken>(token, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginToken> login(@RequestBody Login login, HttpServletRequest request)
    {
//        if(!login.getUsername().endsWith(url))
//            login.setUsername(login.getUsername() + '@' + url);
//        return generateResponse(authService.(login).getBody());
        TrecAccount account = authService.logInUsername(login.getUsername(), login.getPassword());

        logger.info("User Agent String is: {}", request.getHeader("User-Agent"));

        if(account == null)
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        String userToken = jwtTokenService.generateToken(account, request.getHeader("User-Agent"), null);
        String refreshToken = jwtTokenService.generateRefreshToken(account);

        if(userToken == null)
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);


        LoginToken ret = new LoginToken();
        ret.setToken_type("User");
        ret.setAccess_token(userToken);
        ret.setRefresh_token(refreshToken);

        SecurityContext secContext = SecurityContextHolder.createEmptyContext();
        TrecAuthentication tAuth = new TrecAuthentication(account);
        String sessionId = jwtTokenService.getSessionId(ret.getAccess_token());
        logger.info("Session {} generated!", sessionId);
        tAuth.setSessionId(sessionId);
        tAuth.setLoginToken(ret);
        secContext.setAuthentication(tAuth);
        SecurityContextHolder.setContext(secContext);
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }


    @PostMapping("/refresh")
    public ResponseEntity<LoginToken> refreshCode(@RequestBody String refreshToken, @RequestParam("Redirect")String redirect)
    {
        // return generateResponse(authService.getTokenFromRefresh(redirect, refreshToken).getBody());
        return null;
    }


}

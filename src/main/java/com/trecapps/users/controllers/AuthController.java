package com.trecapps.users.controllers;

import com.trecapps.auth.models.LoginToken;
import com.trecapps.auth.models.TokenTime;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.JwtTokenService;
import com.trecapps.auth.services.SessionManager;
import com.trecapps.auth.services.TrecAccountService;
import com.trecapps.users.models.Login;
import com.trecapps.users.models.TokenRequest;
import com.trecapps.users.services.StateService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/Auth")
public class AuthController extends CookieControllerBase{

    @Autowired
    TrecAccountService authService;

    @Autowired
    JwtTokenService jwtTokenService;

    @Autowired
    StateService stateService;

    @Autowired
    SessionManager sessionManager;

    @Value("${base.url}")
    String baseUrl;

    @Value("${tenant.url}")String url;

    Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(@Value("${trecauth.refresh.app:TREC_APPS_REFRESH}") String refreshCookie1,
                          @Value("${trecauth.refresh.domain:#{NULL}}") String domain1,
                          @Value("${trecauth.refresh.on_local:false}") boolean onLocal1) {
        super(refreshCookie1, domain1, onLocal1);
    }

    private ResponseEntity<LoginToken> generateResponse(LoginToken token)
    {
        if(token.getAccess_token() == null || token.getAccess_token().length() == 0)
            return new ResponseEntity<LoginToken>(token, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<LoginToken>(token, HttpStatus.OK);
    }

    @GetMapping("/permissions")
    public List<String> permissions(Authentication authentication)
    {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;

        List<String> ret = new ArrayList<>();
        for(GrantedAuthority ga :trecAuthentication.getAuthorities()) {
            ret.add(ga.getAuthority());
        }
        return ret;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginToken> login(@RequestBody Login login, HttpServletRequest request, HttpServletResponse response)
    {
        TrecAccount account = authService.logInUsername(login.getUsername(), login.getPassword());

        logger.info("User Agent String is: {}", request.getHeader("User-Agent"));

        if(account == null)
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        if(account.getId() == null)
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        TokenTime userToken = jwtTokenService.generateToken(account, request.getHeader("User-Agent"), null, !Boolean.TRUE.equals(login.getStayLoggedIn()));
        String refreshToken = jwtTokenService.generateRefreshToken(account, null, userToken.getSession());

        if(userToken == null)
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);


        LoginToken ret = new LoginToken();
        ret.setToken_type("User");
        ret.setAccess_token(userToken.getToken());
        ret.setRefresh_token(refreshToken);

        OffsetDateTime exp = userToken.getExpiration();
        if(exp != null)
            ret.setExpires_in(exp.getNano() - OffsetDateTime.now().getNano());

        SecurityContext secContext = SecurityContextHolder.createEmptyContext();
        TrecAuthentication tAuth = new TrecAuthentication(account);
        String sessionId = jwtTokenService.getSessionId(ret.getAccess_token());
        logger.info("Session {} generated!", sessionId);
        tAuth.setSessionId(sessionId);
        tAuth.setLoginToken(ret);
        secContext.setAuthentication(tAuth);
        SecurityContextHolder.setContext(secContext);
        this.SetCookie(response, refreshToken);

        return new ResponseEntity<>(ret, HttpStatus.OK);
    }


    @PostMapping("/refresh")
    public ResponseEntity<LoginToken> refreshCode(@RequestBody String refreshToken, @RequestParam("Redirect")String redirect)
    {
        // return generateResponse(authService.getTokenFromRefresh(redirect, refreshToken).getBody());
        return null;
    }


    @GetMapping("/logout")
    public ResponseEntity logout(HttpServletRequest req, HttpServletResponse resp)
    {
        TrecAuthentication trecAuth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();

        String sessionId = trecAuth.getSessionId();

        boolean result = sessionManager.removeSession(trecAuth.getAccount().getId(), sessionId);

        this.RemoveCookie(req, resp);

        return result ? new ResponseEntity(HttpStatus.NO_CONTENT) :
                new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

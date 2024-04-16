package com.trecapps.users.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.trecapps.auth.controllers.CookieBase;
import com.trecapps.auth.models.LoginToken;
import com.trecapps.auth.models.TcUser;
import com.trecapps.auth.models.TokenTime;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.core.JwtTokenService;
import com.trecapps.auth.services.core.SessionManager;
import com.trecapps.auth.services.core.UserStorageService;
import com.trecapps.auth.services.login.TrecAccountService;
import com.trecapps.users.models.Login;
import com.trecapps.users.models.TokenRequest;
import com.trecapps.users.models.UserInfo;
import com.trecapps.users.services.StateService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
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
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/Auth")
public class AuthController //extends CookieControllerBase
{

    TrecAccountService authService;

    String defaultApp;


    SessionManager sessionManager;

    UserStorageService userStorageService;

    JwtTokenService jwtTokenService;

    boolean useCookie;

    String cookieName;

    String domain;

    Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(@Autowired(required = false) CookieBase cookieBase,
                          @Autowired JwtTokenService jwtTokenService1,
                          @Autowired UserStorageService userStorageService1,
                          @Autowired SessionManager sessionManager1,
                          @Autowired TrecAccountService trecAccountService1,
                          @Value("${trecauth.app}") String dApp,
                          @Value("${trecauth.use-cookie:false}")boolean uc,
                          @Value("${trecauth.refresh.cookie-name:TREC_APPS_REFRESH}")String cn,
                          @Value("${trecauth.refresh.domain:#{NULL}}") String domain) {
        this.authService = trecAccountService1;
        this.userStorageService = userStorageService1;
        this.sessionManager = sessionManager1;
        this.jwtTokenService = jwtTokenService1;
        useCookie = uc;
        cookieName = cn;

        defaultApp = dApp;
        this.domain = domain;

        logger.info("CookieBase Provided is {}", cookieBase != null);
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

    @SneakyThrows
    @PostMapping("/login")
    public ResponseEntity<LoginToken> login(
            @RequestBody Login login,
            @RequestParam(value = "app", defaultValue = "") String app,
            HttpServletRequest request,
            HttpServletResponse response)
    {
        if("".equals(app))
            app = defaultApp;

        TrecAccount account = authService.logInUsername(login.getUsername(), login.getPassword());

        TcUser user =userStorageService.retrieveUser(account.getId());

        logger.info("User Agent String is: {}", request.getHeader("User-Agent"));

        if(account == null)
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        if(account.getId() == null)
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        TokenTime userToken = jwtTokenService.generateToken(account, request.getHeader("User-Agent"), null, !Boolean.TRUE.equals(login.getStayLoggedIn()), app);

        if(userToken == null)
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);


        LoginToken ret = new LoginToken();
        ret.setToken_type("User");
        ret.setAccess_token(userToken.getToken());

        OffsetDateTime exp = userToken.getExpiration();
        if(exp != null)
            ret.setExpires_in(exp.getNano() - OffsetDateTime.now().getNano());

        SecurityContext secContext = SecurityContextHolder.createEmptyContext();
        TrecAuthentication tAuth = new TrecAuthentication(user);
        String sessionId = jwtTokenService.getSessionId(ret.getAccess_token());
        logger.info("Session {} generated!", sessionId);
        tAuth.setSessionId(sessionId);
        tAuth.setLoginToken(ret);
        secContext.setAuthentication(tAuth);
        SecurityContextHolder.setContext(secContext);

        if(useCookie)
            tAuth.setUseCookie(true);

        return new ResponseEntity<>(ret, HttpStatus.OK);
    }


    @PostMapping("/refresh")
    public ResponseEntity<LoginToken> refreshCode(@RequestBody String refreshToken, @RequestParam("Redirect")String redirect)
    {
        // return generateResponse(authService.getTokenFromRefresh(redirect, refreshToken).getBody());
        return null;
    }

    void clearSessions(String value, String userId) {
        DecodedJWT decodedJWT = this.jwtTokenService.decodeToken(value);
        if (decodedJWT != null) {
            Map<String, String> sessionList = this.jwtTokenService.claims(decodedJWT);
            sessionList.forEach((_app, s) -> {
                this.sessionManager.removeSession(userId, s);
            });
        }
    }


    @GetMapping("/logout")
    public ResponseEntity logout(HttpServletRequest req, HttpServletResponse resp)
    {
        TrecAuthentication trecAuth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();

        String sessionId = trecAuth.getSessionId();

        boolean result = sessionManager.removeSession(trecAuth.getAccount().getId(), sessionId);

        Cookie[] cookies = req.getCookies();
        if(cookies != null)
            for (Cookie cook: cookies) {
                if (cook.getName().equals(this.cookieName)) {
                    this.clearSessions(cook.getValue(), trecAuth.getAccount().getId());
                    cook.setValue("");
                    cook.setPath("/");
                    cook.setMaxAge(0);
                    if(domain != null)
                        cook.setDomain(domain);
                    resp.addCookie(cook);

                }
            }

        return result ? new ResponseEntity(HttpStatus.NO_CONTENT) :
                new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @SneakyThrows
    @GetMapping("/User")
    ResponseEntity<UserInfo> getUserInfo(Authentication authentication)
    {
        TrecAuthentication authentication1 = (TrecAuthentication) authentication;
        TrecAccount account = authentication1.getAccount();

        UUID brandUuid = authentication1.getBrandId();

        String brandId = brandUuid == null ? null : brandUuid.toString();

        UserInfo ret = new UserInfo();
        ret.setUser(userStorageService.retrieveUser(account.getId()));
        if(brandId != null)
            ret.setBrand(userStorageService.retrieveBrand(brandId));

        return new ResponseEntity<>(ret, HttpStatus.OK);
    }


}

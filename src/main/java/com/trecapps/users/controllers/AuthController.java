package com.trecapps.users.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.trecapps.auth.webflux.controllers.CookieBase;
import com.trecapps.auth.common.models.*;
import com.trecapps.auth.common.models.primary.TrecAccount;
import com.trecapps.auth.webflux.services.JwtTokenServiceAsync;
import com.trecapps.auth.webflux.services.V2SessionManagerAsync;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.trecapps.auth.webflux.services.TrecAccountServiceAsync;
import com.trecapps.auth.webflux.services.TrecSecurityContextReactive;
import com.trecapps.users.models.IdBodyExtender;
import com.trecapps.users.models.Login;
import com.trecapps.users.models.UserInfo;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.*;


@RestController
@RequestMapping("/Auth")
public class AuthController //extends CookieControllerBase
{

    TrecAccountServiceAsync authService;

    String defaultApp;


    V2SessionManagerAsync sessionManager;

    IUserStorageServiceAsync userStorageService;

    JwtTokenServiceAsync jwtTokenService;

    TrecSecurityContextReactive securityContextServlet;

    boolean useCookie;
    CookieBase cookieBase;

    String cookieName;

    String domain;

    Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(@Autowired(required = false) CookieBase cookieBase,
                          @Autowired JwtTokenServiceAsync jwtTokenService1,
                          @Autowired IUserStorageServiceAsync userStorageService1,
                          @Autowired V2SessionManagerAsync sessionManager1,
                          @Autowired TrecAccountServiceAsync trecAccountService1,
                          @Autowired TrecSecurityContextReactive trecSecurityContextServlet1,
                          @Value("${trecauth.app}") String dApp,
                          @Value("${trecauth.use-cookie:false}")boolean uc,
                          @Value("${trecauth.refresh.cookie-name:TREC_APPS_REFRESH}")String cn,
                          @Value("${trecauth.refresh.domain:#{NULL}}") String domain) {
        this.authService = trecAccountService1;
        this.userStorageService = userStorageService1;
        this.sessionManager = sessionManager1;
        this.jwtTokenService = jwtTokenService1;
        this.securityContextServlet = trecSecurityContextServlet1;
        this.cookieBase = cookieBase;
        useCookie = uc;
        cookieName = cn;

        defaultApp = dApp;
        this.domain = domain;

        logger.info("CookieBase Provided is {}", cookieBase != null);
    }

    private ResponseEntity<LoginToken> generateResponse(LoginToken token)
    {
        if(token.getAccess_token() == null || token.getAccess_token().isEmpty())
            return new ResponseEntity<LoginToken>(token, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<LoginToken>(token, HttpStatus.OK);
    }

    @GetMapping("/permissions")
    public Mono<List<String>> permissions(Authentication authentication)
    {
        return Mono.just((TrecAuthentication) authentication)
                .map((TrecAuthentication trecAuthentication) -> {
                    List<String> ret = new ArrayList<>();
                    for(GrantedAuthority ga :trecAuthentication.getAuthorities()) {
                        ret.add(ga.getAuthority());
                    }
                    return ret;
                });
    }

    @SneakyThrows
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginToken>> login(
            @RequestBody Login login,
            @RequestParam(value = "app", defaultValue = "") String app,
            ServerWebExchange exchange)
    {
        if("".equals(app))
            app = defaultApp;

        String finalApp = app;
        return authService.logInUsername(login.getUsername(), login.getPassword())
                .flatMap((Optional<TrecAccount> accountOpt) -> {
                            if (accountOpt.isEmpty())
                                throw new ResponseEntityException(null, HttpStatus.UNAUTHORIZED);
                            TrecAccount account = accountOpt.get();
                            if(account.isInvalid())
                                throw new ResponseEntityException(null, HttpStatus.UNAUTHORIZED);
                            if (account.getId() == null)
                                throw new ResponseEntityException(null, HttpStatus.FORBIDDEN);

                            return jwtTokenService
                                    .generateToken(
                                            account,
                                            exchange.getRequest().getHeaders().get("User-Agent").get(0),
                                            null,
                                            !Boolean.TRUE.equals(login.getStayLoggedIn()),
                                            finalApp)
                                    .map((Optional<TokenTime> tt) ->
                                            tt.<IdBodyExtender<TokenTime>>map(tokenTime -> new IdBodyExtender<>(tokenTime, account.getId(), account.getUsername())).orElseGet(() -> new IdBodyExtender<>(account.getId(), account.getUsername())))
                                    ;

                        })
                .flatMap((IdBodyExtender<TokenTime> userTokenOpt) -> {
                    if(userTokenOpt.isEmpty())
                        return Mono.just(new ResponseEntity<LoginToken>(HttpStatus.INTERNAL_SERVER_ERROR));
                    TokenTime ut = userTokenOpt.getFullBody();



                    Mono<TokenTime> monoRet = defaultApp.equals(finalApp) ? Mono.just(ut) :
                            this.sessionManager.setBrandMono(userTokenOpt.getId(), ut.getSession(), null, defaultApp, false)
                                    .then(Mono.just(ut));

                    return monoRet.map((TokenTime userToken) -> {
                        LoginToken ret = new LoginToken();
                        ret.setToken_type("User");
                        ret.setAccess_token(userToken.getToken());
                        TrecAccount account = new TrecAccount();
                        account.setId(userTokenOpt.getId());
                        account.setUsername(userTokenOpt.getUserName());
                        ret.setRefresh_token(this.jwtTokenService.generateRefreshToken(account, userToken.getSession()));
                        OffsetDateTime exp = userToken.getExpiration();
                        if(exp != null)
                            ret.setExpires_in(exp.getNano() - OffsetDateTime.now().getNano());

                        if(useCookie && cookieBase != null && Boolean.TRUE.equals(login.getStayLoggedIn())){
                            cookieBase.SetCookie(exchange.getResponse(), ret.getRefresh_token());
                        }

                        return new ResponseEntity<LoginToken>(ret, HttpStatus.OK);
                    });


                });

    }


//    @PostMapping("/refresh")
//    public ResponseEntity<LoginToken> refreshCode(@RequestBody String refreshToken, @RequestParam("Redirect")String redirect)
//    {
//        // return generateResponse(authService.getTokenFromRefresh(redirect, refreshToken).getBody());
//        return null;
//    }

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
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange, Authentication authentication)
    {
        ServerHttpRequest req = exchange.getRequest();
        ServerHttpResponse resp = exchange.getResponse();
        TrecAuthentication trecAuth = (TrecAuthentication) authentication;

        String sessionId = trecAuth.getSessionId();

        return Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT))
                .doOnNext(re -> {
                    sessionManager.removeSession(trecAuth.getAccount().getId(), sessionId);
                    if(useCookie && cookieBase != null)
                        cookieBase.RemoveCookie(resp, req, trecAuth.getAccount().getId());
                })
                .onErrorResume((Throwable thrown) -> Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }



    @SneakyThrows
    @GetMapping("/User")
    Mono<ResponseEntity<UserInfo>> getUserInfo(Authentication authentication)
    {
        TrecAuthentication authentication1 = (TrecAuthentication) authentication;
        TrecAccount account = authentication1.getAccount();

        UUID brandUuid = authentication1.getBrandId();

        String brandId = brandUuid == null ? null : brandUuid.toString();

        UserInfo ret = new UserInfo();

        return Mono.just(ret)
                        .flatMap((UserInfo r) -> {
                            return userStorageService.getAccountById(account.getId())
                                    .map((Optional<TcUser> oUser) -> {
                                        if(oUser.isPresent())
                                            r.setUser(oUser.get());
                                        return r;
                                    });
                        }).flatMap((UserInfo r) -> {
                          if(brandId != null)
                          {
                              return userStorageService.getBrandById(brandId)
                                      .map((Optional<TcBrands> oBrand) -> {
                                          if(oBrand.isPresent())
                                              r.setBrand(oBrand.get());
                                          return r;
                                      });
                          }
                          return Mono.just(r);
                        }).map(ResponseEntity::ok);

    }


}

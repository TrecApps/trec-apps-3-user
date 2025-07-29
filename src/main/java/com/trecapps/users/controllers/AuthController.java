package com.trecapps.users.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
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

    boolean isMfaRequired(TcUser user, String app) {
        for(MfaReq req: user.getMfaRequirements())
        {
            if(app.equals(req.getApp()))
                return req.isRequireMfa();
        }
        return false;
    }

    boolean needsMfa(String app, TcUser user) {
        List<MfaReq> reqs = user.getMfaRequirements();
        if(reqs == null) return false;

        for(MfaReq req: reqs){
            if(app.equals(req.getApp()))
                return req.isRequireMfa();
        }
        return false;
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
        return authService.logInUsername(login.getUsername(), login.getPassword(), defaultApp)
                .flatMap((Optional<TrecAccount> accountOpt) -> {
                            if (accountOpt.isEmpty())
                                throw new ResponseEntityException(null, HttpStatus.UNAUTHORIZED);
                            TrecAccount account = accountOpt.get();
                            if(account.isInvalid())
                                throw new ResponseEntityException(null, HttpStatus.UNAUTHORIZED);
                            if (account.getId() == null)
                                throw new ResponseEntityException(null, HttpStatus.FORBIDDEN);




                            return userStorageService.getAccountById(account.getId())
                                    .flatMap((Optional<TcUser> optUser) -> {
                                        TcUser user = optUser.get();
                                        TokenOptions options = new TokenOptions();
                                        options.setExpires(!Boolean.TRUE.equals(login.getStayLoggedIn()));
                                        options.setNeedsMfa(isMfaRequired(user, finalApp));

                                        JsonNode extensions = user.getExtensions();

                                        String brandId = null;
                                        if(extensions != null
                                        // ToDo: see if the user has configured a Brand account for a specific app, once it has been set up
                                        ) {

                                        }

                                        if(brandId == null && user.isAutoBrandAccount())
                                        {
                                            brandId = user.getDedicatedBrandAccount();
                                        }

                                        return brandId == null ? jwtTokenService.generateToken(
                                                account,
                                                exchange.getRequest().getHeaders().get("User-Agent").get(0),
                                                null,
                                                finalApp,
                                                options) : this.userStorageService.getBrandById(brandId)
                                                .flatMap((Optional<TcBrands> oBrands) ->
                                                    jwtTokenService.generateToken(
                                                            account,
                                                            exchange.getRequest().getHeaders().get("User-Agent").get(0),
                                                            oBrands.orElse(null),
                                                            finalApp,
                                                            options)
                                                );

                                    }).map((Optional<TokenTime> tt) ->
                                            tt.<IdBodyExtender<TokenTime>>map(tokenTime -> new IdBodyExtender<>(tokenTime, account.getId(), account.getUsername())).orElseGet(() -> new IdBodyExtender<>(account.getId(), account.getUsername())))
                                    ;


                        })
                .flatMap((IdBodyExtender<TokenTime> userTokenOpt) -> {
                    if(userTokenOpt.isEmpty())
                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    TokenTime ut = userTokenOpt.getFullBody();



                    Mono<TokenTime> monoRet = defaultApp.equals(finalApp) ? Mono.just(ut) :
                            this.sessionManager.setBrandMono(userTokenOpt.getId(), ut.getSession(), null, defaultApp, false)
                                    .then(Mono.just(ut));

                    return monoRet.flatMap((TokenTime userToken) -> {
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

                        return userStorageService.getAccountById(account.getId())
                                .map((Optional<TcUser> oUser) -> {
                                    if(oUser.isEmpty()) return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

                                    TcUser user = oUser.get();
                                    if(isMfaRequired(user, finalApp)){
                                        ret.setToken_type("User-requires_mfa");
                                    }

                                    return ResponseEntity.ok(ret);
                                });


                    });


                });

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


    String maskEmail(String email){
        if(email == null) return "profile@example.com";

        String[] pieces = email.split("@", 2);

        if(pieces[0].length() > 3)
            pieces[0] = pieces[0].substring(0, 3) + "xxxx";
        else pieces[0] = pieces[0] + "xxxx";
        return pieces[0] + "@" + pieces[1];
    }

    PhoneNumber maskNumber(PhoneNumber number){
        if(number == null) return null;

        String rawNumber = number.getNumber();
        StringBuilder newNumber = new StringBuilder();
        for(int c = 0; c < rawNumber.length() - 1; c++){
            newNumber.append(c < rawNumber.length() - 5 ? '0' : rawNumber.charAt(c));
        }

        number.setNumber(Long.parseLong(newNumber.toString()));
        return number;

    }

    @SneakyThrows
    @GetMapping("/User")
    Mono<ResponseEntity<UserInfo>> getUserInfo(Authentication authentication)
    {
        TrecAuthentication authentication1 = (TrecAuthentication) authentication;

        UserInfo ret = new UserInfo();

        TcBrands brands = authentication1.getBrand();
        TcUser user = authentication1.getUser();
        ret.setUser(user);
        ret.setBrand(brands);

        return Mono.just(ret)
                        .flatMap((UserInfo r) -> {

                          return Mono.just(r).doOnNext((UserInfo ui) -> {
                              TcUser u = ui.getUser();
                              if(u == null) return;

                              u.getMfaMechanisms().forEach((MfaMechanism mech) ->{
                                  mech.setUserCode(null);
                                  mech.setCode(null);
                                  mech.setExpires(null);
                              });
                          });
                        })
                .flatMap((UserInfo userInfo) -> {
                    if(userInfo.getBrand() != null) return Mono.just(userInfo);

                    TcUser user1 = userInfo.getUser();

                    if(user1.getDedicatedBrandAccount() != null && user1.isAutoBrandAccount()){
                        return this.userStorageService.getBrandById(user1.getDedicatedBrandAccount())
                                .map((Optional<TcBrands> oBrands) -> {
                                    oBrands.ifPresent(userInfo::setBrand);
                                    return userInfo;
                                });
                    }
                    return Mono.just(userInfo);
                })
                .doOnNext((UserInfo retInfo) -> {
                    if(authentication1.isNeedsMfa()){
                        retInfo.setBrand(null);
                        TcUser user1 = retInfo.getUser();
                        user1.setCredibilityRating(0);
                        user1.setAddress(new ArrayList<>());
                        user1.setAuthRoles(new ArrayList<>());
                        user1.setBirthday(OffsetDateTime.now());
                        user1.setBirthdaySetting("PRIVATE");
                        user1.setBrands(new HashSet<>());
                        user1.setBrandSettings(new HashMap<>());
                        user1.setCodeExpiration(OffsetDateTime.now());
                        user1.setAddressList(new AddressList());
                        user1.setCustomerId(null);
                        user1.setDisplayName("");
                        user1.setVerificationCodes(new HashMap<>());
                        user1.setUserProfile("");
                        user1.setRestrictions("");
                        user1.setProposedEmail(null);
                        user1.setProfilePics(new HashMap<>());
                        user1.setProposedNumber(null);
                        user1.setPastEmails(new HashSet<>());
                        user1.setEmail(maskEmail(user1.getEmail()));
                        user1.setPartition(0);
                        user1.setMfaRequirements(new ArrayList<>());
                        user1.setVerifiedEmail(maskEmail(user1.getVerifiedEmail()));
                        user1.setVerifiedNumber(maskNumber(user1.getVerifiedNumber()));

                        user1.setAuthorities(new ArrayList<>());

                        user1.setMobilePhone(maskNumber(user1.getMobilePhone()));
                        user1.setProfilePic(null);
                        user1.setCurrentCode(null);
                        user1.setSubscriptionId(null);

                    }
                })
                .map(ResponseEntity::ok);

    }


}

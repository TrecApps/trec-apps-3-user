package com.trecapps.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.webflux.controllers.CookieBase;
import com.trecapps.auth.common.models.LoginToken;
import com.trecapps.auth.common.models.TcUser;
import com.trecapps.auth.common.models.TokenTime;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.common.models.primary.TrecAccount;
import com.trecapps.auth.webflux.services.JwtTokenServiceAsync;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.trecapps.auth.webflux.services.TrecAccountServiceAsync;
import com.trecapps.users.models.AuthenticationBody;
import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.UserPost;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/Users")
public class UserController extends CookieControllerBase{

    TrecAccountServiceAsync userService;

    IUserStorageServiceAsync userStorageService;


    @Value("${trecauth.app}") String defaultApp;


    public UserController(
            @Autowired(required = false) CookieBase cookieBase,
            @Autowired TrecAccountServiceAsync userService,
            @Autowired IUserStorageServiceAsync userStorageService1,
            @Autowired JwtTokenServiceAsync jwtTokenService1,
            @Value("${trecauth.refresh.domain}") String domain,
            @Value("${trecauth.refresh.cookie-name:TREC_APPS_REFRESH}") String cookieName)
    {
        super(cookieBase, jwtTokenService1, domain, cookieName);
        this.userService = userService;
        this.userStorageService = userStorageService1;
    }


    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/createUser")
    public Mono<ResponseEntity> createNewUser(
            RequestEntity<UserPost> post,
            ServerWebExchange exchange)
    {

        return Mono.just(post.getBody())
                .flatMap((UserPost postBody) -> {
                    logger.info("Creating logger with User Principal name {}", postBody.getUserPrincipalName());


                            // To-Do: Add Validation to the User Post

                            // End To-Do

                    TrecAccount newAccount = new TrecAccount();
                    newAccount.setPasswordHash(postBody.getPasswordProfile().getPassword());
                    newAccount.setUsername(postBody.getUserPrincipalName());



                    return userService.saveNewAccount(newAccount);
                }).flatMap((Optional<TrecAccount> newAccountOpt) -> {
                    if (newAccountOpt.isEmpty())
                        throw new ResponseEntityException("Account Exists", HttpStatus.BAD_REQUEST);
                    TrecAccount newAccount = newAccountOpt.get();
                    TcUser user = post.getBody().GetTcUserObject();
                    user.setId(newAccount.getId());
                    userStorageService.saveUser(user);



                    return jwtTokenService.generateToken(newAccount, exchange.getRequest().getHeaders().get("User-Agent").get(0), null, false, defaultApp);
                }).map((Optional<TokenTime> userTokenOpt) -> {
                    if(userTokenOpt.isEmpty())
                        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                    TcUser user = post.getBody().GetTcUserObject();
                    LoginToken token = new LoginToken();
                    token.setToken_type("User");
                    TokenTime userToken = userTokenOpt.get();
                    token.setAccess_token(userToken.getToken());
                    //token.setRefresh_token(refreshToken);

                    OffsetDateTime exp = userToken.getExpiration();
                    if(exp != null)
                        token.setExpires_in(exp.getNano() - OffsetDateTime.now().getNano());
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    TrecAuthentication tAuth = new TrecAuthentication(user);

                    tAuth.setLoginToken(token);
                    String sessionId = jwtTokenService.getSessionId(token.getAccess_token());
                    logger.info("Session {} generated!", sessionId);
                    tAuth.setSessionId(sessionId);
                    context.setAuthentication(tAuth);
                    SecurityContextHolder.setContext(context);

                    if(cookieBase != null)
                        applyCookie(tAuth, token, exchange.getResponse());

                    return new ResponseEntity(token, HttpStatus.OK);
                })
                .onErrorResume(ResponseEntityException.class, ResponseEntityException::getEntityMono);

    }


    @PutMapping(value = "/UserUpdate", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> updateUser(RequestEntity<TcUser> post)
    {
        TcUser user = post.getBody();
        // To-Do: Make sure post id and uer id match
        SecurityContext context = SecurityContextHolder.getContext();
        TrecAuthentication auth = (TrecAuthentication) context.getAuthentication();

        return Mono.just(new AuthenticationBody<TcUser>(auth, user))
                .map((AuthenticationBody<TcUser> authBody) -> {
                    TrecAuthentication authentication = authBody.getAuthentication();
                    TcUser user1 = authBody.getData();
                    if(!authentication.getAccount().getId().equals(user1.getId()))
                        return new ResponseEntity<String>("Mismatched IDs", HttpStatus.FORBIDDEN);
                    TcUser existingUser = authentication.getUser();
                    user1.setPhoneVerified(existingUser.isPhoneVerified());
                    user1.setRestrictions(existingUser.getRestrictions());
                    user1.setEmailVerified(existingUser.isEmailVerified());
                    user1.setCredibilityRating(existingUser.getCredibilityRating());

                    // Don't allow User to Update the birthday on a whim. If a mistake was made,
                    // have an employee make the update
                    user1.setBirthday(existingUser.getBirthday());

                    if(user1.getBirthdaySetting() == null)
                        user1.setBirthdaySetting(existingUser.getBirthdaySetting());

                    // To-Do: continue Checks


                    // End To-Do, here, assume all checks re valid

                    userStorageService.saveUser(user1);
                    return new ResponseEntity<>("Success", HttpStatus.OK);
                });

    }

    @GetMapping("/Current")
    public Mono<ResponseEntity<TcUser>> getUser(Authentication authentication)
    {
        return Mono.just(authentication)
                        .map(auth -> (TrecAuthentication)auth)
                                .map((trecAuthentication) -> ResponseEntity.ok(trecAuthentication.getUser()));

    }

    @PostMapping("/passwordUpdate")
    public Mono<ResponseEntity<String>> updatePassword(RequestEntity<PasswordChange> post, Authentication auth)
    {
        return Mono.just(new AuthenticationBody<PasswordChange>((TrecAuthentication)auth,post.getBody()))
                .map((AuthenticationBody<PasswordChange> pc) -> {
                    return userService.changePassword(
                            pc.getAuthentication().getAccount(),
                            pc.getData().getCurrentPassword(),
                            pc.getData().getNewPassword());
                }).map((Boolean result) -> new ResponseEntity<>(result ? "Success" : "Failed!", result ? HttpStatus.OK :HttpStatus.UNAUTHORIZED));
    }
}

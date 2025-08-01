package com.trecapps.users.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trecapps.auth.common.encryptors.IFieldEncryptor;
import com.trecapps.auth.common.models.*;
import com.trecapps.auth.webflux.controllers.CookieBase;
import com.trecapps.auth.common.models.primary.TrecAccount;
import com.trecapps.auth.webflux.services.JwtTokenServiceAsync;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.trecapps.auth.webflux.services.TrecAccountServiceAsync;
import com.trecapps.users.models.*;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/Users")
public class UserController extends CookieControllerBase{

    TrecAccountServiceAsync userService;

    IUserStorageServiceAsync userStorageService;

    @Autowired
    IFieldEncryptor encryptor;


    @Value("${trecauth.app}") String defaultApp;

    @Value("${trec-apps.styles}")
    String[] styles;


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

                    // Maintain compatibility
                    user.setProposedEmail(user.getEmail());
                    user.setProposedNumber(user.getMobilePhone());
                    user.setAddressList(new AddressList());

                    userStorageService.saveUser(user);



                    return jwtTokenService.generateToken(
                            newAccount,
                            exchange.getRequest().getHeaders().get("User-Agent").get(0),
                            null,
                            defaultApp,
                            new TokenOptions());
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

    String join(String[] strings){
        if(strings == null || strings.length == 0) return "";

        StringBuilder ret = new StringBuilder(strings[0].trim());

        for(int c = 1; c < strings.length; c++){
            ret.append(",").append(strings[c].trim());
        }

        return ret.toString();
    }

    @PatchMapping("/styles")
    public Mono<ResponseEntity<ResponseObj>> updateStyle(@RequestBody StyleSpec styleSpec, @RequestParam String app, Authentication authentication){

        return Mono.just(authentication)
                .map((Authentication auth) -> (TrecAuthentication)auth)
                .flatMap((TrecAuthentication tAuth) -> {
                    return this.userStorageService.getAccountById(tAuth.getUser().getId());
                })
                .map((Optional<TcUser> oUser) -> {
                    if (oUser.isEmpty())
                        throw new ObjectResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't find User Info");
                    return oUser.get();
                })
                .flatMap((TcUser user) -> {
                    boolean found = false;

                    StyleSpec spec = new StyleSpec();
                    spec.setUseDark(styleSpec.isUseDark());

                    for(String style: styles){
                        if(style.equals(styleSpec.getStyle())){
                            spec.setStyle(style);
                            break;
                        }
                    }

                    if(spec.getStyle() == null){
                        throw new ObjectResponseException(HttpStatus.BAD_REQUEST,
                                String.format("Provided Style not supported. Please choose from one of the following: %s",join( this.styles))
                                );
                    }

                    JsonNode extensions = user.getExtensions();
                    if(!(extensions instanceof ObjectNode oExtensions))
                        throw new ObjectResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "User Extensions field mismatch");

                    JsonNode jNode = oExtensions.get("styles");

                    ObjectNode oNode;

                    if(!(jNode instanceof ObjectNode)){
                        oNode = new ObjectNode(new JsonNodeFactory(false));
                        jNode = oNode;
                        oExtensions.set("styles", jNode);
                    } else oNode = (ObjectNode) jNode;

                    oNode.set(app, spec.getObjectAsNode());

                    return userStorageService.saveUserMono(user).thenReturn(ResponseObj.getInstance("Updated"));
                })
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException e) -> Mono.just(e.toResponseObject()))
                .map(ResponseObj::toEntity);

    }

    @PutMapping(value = "/UserUpdate", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> updateUser(RequestEntity<TcUser> post, Authentication a)
    {
        TcUser user = post.getBody();
        TrecAuthentication auth = (TrecAuthentication) a;

        return Mono.just(new AuthenticationBody<TcUser>(auth, user))
                .map((AuthenticationBody<TcUser> authBody) -> {
                    TrecAuthentication authentication = authBody.getAuthentication();
                    TcUser user1 = authBody.getData();
                    if(!authentication.getAccount().getId().equals(user1.getId()))
                        return new ResponseEntity<String>("Mismatched IDs", HttpStatus.FORBIDDEN);
                    TcUser existingUser = authentication.getUser();
                    user1.setRestrictions(existingUser.getRestrictions());
                    assert user != null;

                    user1.setMfaMechanisms(existingUser.getMfaMechanisms());
                    managePhoneAndEmailChanges(user, user1);
                    user1.setCredibilityRating(existingUser.getCredibilityRating());
                    user1.setId(existingUser.getId());
                    user1.setVerificationCodes(existingUser.getVerificationCodes());
                    user1.setAuthRoles(existingUser.getAuthRoles());
                    user1.setMfaRequirements(existingUser.getMfaRequirements());

                    user1.setSubscriptionId(existingUser.getSubscriptionId());
                    user1.setCustomerId(existingUser.getCustomerId());
                    user1.setAddressList(existingUser.getAddressList());

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

    void managePhoneAndEmailChanges(TcUser postedUser, TcUser existingUser){
        // For the new system, do not update the verified email/phone here
        postedUser.setVerifiedEmail(existingUser.getVerifiedEmail());
        postedUser.setVerifiedNumber(existingUser.getVerifiedNumber());
        postedUser.setPastEmails(existingUser.getPastEmails());

        if(!areStringsEqual(postedUser.getEmail(), existingUser.getEmail())){
            existingUser.setEmailVerified(false);
            existingUser.setEmail(postedUser.getEmail());

            // Since Email is not currently (or no longer) verified, make sure that Email can't be used for MFA
            existingUser.setMfaMechanisms(
                    existingUser.getMfaMechanisms()
                            .stream()
                            .filter((MfaMechanism mech) -> !mech.getSource().equals("Email"))
                            .toList()
            );
        }

        if(!areStringsEqual(postedUser.getProposedEmail(), existingUser.getVerifiedEmail())){
            existingUser.setProposedEmail(postedUser.getProposedEmail());

            // Since Email is not currently (or no longer) verified, make sure that Email can't be used for MFA
            existingUser.setMfaMechanisms(
                    existingUser.getMfaMechanisms()
                            .stream()
                            .filter((MfaMechanism mech) -> !mech.getSource().equals("Email"))
                            .toList()
            );
        }


        if(!areObjectsEqual(postedUser.getMobilePhone(), existingUser.getMobilePhone())){
            existingUser.setPhoneVerified(false);
            existingUser.setMobilePhone(postedUser.getMobilePhone());

            // Since Email is not currently (or no longer) verified, make sure that Email can't be used for MFA
            existingUser.setMfaMechanisms(
                    existingUser.getMfaMechanisms()
                            .stream()
                            .filter((MfaMechanism mech) -> !mech.getSource().equals("Phone"))
                            .toList()
            );
        }

        if(!areObjectsEqual(postedUser.getProposedNumber(), existingUser.getVerifiedNumber())){
            existingUser.setProposedNumber(postedUser.getProposedNumber());

            // Since Email is not currently (or no longer) verified, make sure that Email can't be used for MFA
            existingUser.setMfaMechanisms(
                    existingUser.getMfaMechanisms()
                            .stream()
                            .filter((MfaMechanism mech) -> !mech.getSource().equals("Phone"))
                            .toList()
            );
        }
    }

    boolean areStringsEqual(String str1, String str2){
        if(str1 == null) return str2 == null;
        return str1.trim().equals(str2 == null ? null : str2.trim());
    }

    boolean areObjectsEqual(Object obj1, Object obj2) {
        if(obj1 == null) return obj2 == null;
        return obj1.equals(obj2);
    }

    Mono<TcUser> callibrateUser(TcUser user) {
        boolean callibrate = false;
        if(user.isEmailVerified() && user.getVerifiedEmail() == null){
            callibrate = true;
            user.setProposedEmail(user.getEmail());
            user.setVerifiedEmail(user.getEmail());
        }

        if(user.isPhoneVerified() && user.getVerifiedNumber() == null){
            callibrate = true;
            user.setProposedEmail(user.getEmail());
            user.setVerifiedEmail(user.getEmail());
        }

        if(callibrate)
            return userStorageService.saveUserMono(user).thenReturn(user).doOnNext((TcUser user1) -> encryptor.decrypt(user1));
        return Mono.just(user);
    }

    @GetMapping("/Current")
    public Mono<ResponseEntity<TcUser>> getUser(Authentication authentication)
    {
        return Mono.just(authentication)
                        .map(auth -> (TrecAuthentication)auth)
                        .flatMap((trecAuthentication) -> {
                                    TcUser user = trecAuthentication.getUser();

                                    return callibrateUser(user).doOnNext((TcUser u) -> {
                                        u.getMfaMechanisms().forEach((MfaMechanism mech) -> {
                                            mech.setUserCode(null);
                                            mech.setCode(null);
                                            mech.setExpires(null);
                                        });
                                    });
                        })

                        .map(ResponseEntity::ok);

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

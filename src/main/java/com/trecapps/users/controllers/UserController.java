package com.trecapps.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.controllers.CookieBase;
import com.trecapps.auth.models.LoginToken;
import com.trecapps.auth.models.TcUser;
import com.trecapps.auth.models.TokenTime;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.core.JwtTokenService;
import com.trecapps.auth.services.core.UserStorageService;
import com.trecapps.auth.services.login.TrecAccountService;
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
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/Users")
public class UserController extends CookieControllerBase{

    TrecAccountService userService;

    UserStorageService userStorageService;


    @Value("${trecauth.app}") String defaultApp;


    public UserController(
            @Autowired(required = false) CookieBase cookieBase,
            @Autowired TrecAccountService userService,
            @Autowired UserStorageService userStorageService1,
            @Autowired JwtTokenService jwtTokenService1)
    {
        super(cookieBase, jwtTokenService1);
        this.userService = userService;
        this.userStorageService = userStorageService1;
    }


    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/createUser")
    public ResponseEntity createNewUser(
            RequestEntity<UserPost> post,
            HttpServletRequest request,
            HttpServletResponse response)
    {
        logger.info("Creating New User!");
        UserPost postBody = post.getBody();

        logger.info("Creating logger with User Principal name {}", postBody.getUserPrincipalName());

        // To-Do: Add Validation to the User Post

        // End To-Do

        TrecAccount newAccount = new TrecAccount();
        newAccount.setPasswordHash(postBody.getPasswordProfile().getPassword());
        newAccount.setUsername(postBody.getUserPrincipalName());



        newAccount =  userService.saveNewAccount(newAccount);

        if(newAccount == null)
        {
            return new ResponseEntity("Account Exists", HttpStatus.BAD_REQUEST);
        }

        TcUser user = postBody.GetTcUserObject();
        user.setId(newAccount.getId());
        userStorageService.saveUser(user);


        LoginToken token = new LoginToken();
        //sessionM

        TokenTime userToken = jwtTokenService.generateToken(newAccount, request.getHeader("User-Agent"), null, false, defaultApp);
        //String refreshToken = jwtTokenService.generateRefreshToken(newAccount, null, userToken.getSession());

        if(userToken == null)
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);


        token.setToken_type("User");
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
            applyCookie(tAuth, token, response);

        return new ResponseEntity(token, HttpStatus.OK);
    }


    @PutMapping(value = "/UserUpdate", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> updateUser(RequestEntity<TcUser> post)
    {
        TcUser user = post.getBody();
        // To-Do: Make sure post id and uer id match
        SecurityContext context = SecurityContextHolder.getContext();
        TrecAuthentication auth = (TrecAuthentication) context.getAuthentication();

        if(!auth.getAccount().getId().equals(user.getId()))
            return new ResponseEntity<>("Mismatched IDs", HttpStatus.FORBIDDEN);
        // ENd to-Do:

        try {
            com.trecapps.auth.models.TcUser existingEntry = userStorageService.retrieveUser(user.getId());

            user.setPhoneVerified(existingEntry.isPhoneVerified());
            user.setRestrictions(existingEntry.getRestrictions());
            user.setEmailVerified(existingEntry.isEmailVerified());
            user.setCredibilityRating(existingEntry.getCredibilityRating());

            // To-Do: continue Checks


            // End To-Do, here, assume all checks re valid

            userStorageService.saveUser(user);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    @GetMapping("/Current")
    public ResponseEntity<TcUser> getUser()
    {
        logger.info("Retrieving User");

        try {
            TcUser user = userStorageService.retrieveUser(((TrecAuthentication) SecurityContextHolder.getContext().getAuthentication()).getAccount().getId());

            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch(NullPointerException e)
        {
            logger.error("Null Pointer Exception Detected! ",e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch(JsonProcessingException e)
        {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/passwordUpdate")
    public ResponseEntity<String> updatePassword(RequestEntity<PasswordChange> post)
    {
        PasswordChange change = post.getBody();
        ;
        boolean result = userService.changePassword(
                ((TrecAuthentication)SecurityContextHolder.getContext().getAuthentication()).getAccount(),
                change.getCurrentPassword(),
                change.getNewPassword());

        return new ResponseEntity<>(result ? "Success" : "Failed!", result ? HttpStatus.OK :HttpStatus.UNAUTHORIZED);
    }
}

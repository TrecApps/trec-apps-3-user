package com.trecapps.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.models.LoginToken;
import com.trecapps.auth.models.TokenTime;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.JwtTokenService;
import com.trecapps.auth.services.SessionManager;
import com.trecapps.auth.services.TrecAccountService;
import com.trecapps.auth.services.UserStorageService;
import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.TcUser;
import com.trecapps.users.models.UserPost;
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

import javax.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/Users")
public class UserController {

    TrecAccountService userService;

    UserStorageService userStorageService;

    JwtTokenService jwtTokenService;

    String url;

    public UserController(@Value("${tenant.url}")String url,
            @Autowired TrecAccountService userService,
            @Autowired UserStorageService userStorageService1,
            @Autowired JwtTokenService jwtTokenService1)
    {
        this.userService = userService;
        this.url = url;
        this.userStorageService = userStorageService1;
        this.jwtTokenService = jwtTokenService1;
    }


    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/createUser")
    public ResponseEntity createNewUser(RequestEntity<UserPost> post, HttpServletRequest request)
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

        userStorageService.saveUser(new TcUser(postBody, newAccount.getId()).getAuthUser());



        LoginToken token = new LoginToken();
        //sessionM

        TokenTime userToken = jwtTokenService.generateToken(newAccount, request.getHeader("User-Agent"), null, false);
        String refreshToken = jwtTokenService.generateRefreshToken(newAccount, null, userToken.getSession());

        if(userToken == null)
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);


        LoginToken ret = new LoginToken();
        ret.setToken_type("User");
        ret.setAccess_token(userToken.getToken());
        ret.setRefresh_token(refreshToken);

        OffsetDateTime exp = userToken.getExpiration();
        if(exp != null)
            ret.setExpires_in(exp.getNano() - OffsetDateTime.now().getNano());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        TrecAuthentication tAuth = new TrecAuthentication(newAccount);

        tAuth.setLoginToken(token);
        String sessionId = jwtTokenService.getSessionId(token.getAccess_token());
        logger.info("Session {} generated!", sessionId);
        tAuth.setSessionId(sessionId);
        context.setAuthentication(tAuth);
        SecurityContextHolder.setContext(context);

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

            userStorageService.saveUser(user.getAuthUser());
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
            com.trecapps.auth.models.TcUser user = userStorageService.retrieveUser(((TrecAuthentication) SecurityContextHolder.getContext().getAuthentication()).getAccount().getId());

            return new ResponseEntity<>(TcUser.getUserFromAuthUser(user), HttpStatus.OK);
        } catch(NullPointerException e)
        {
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
                change.getCurrentPassword().toString(),
                change.getNewPassword().toString());

        return new ResponseEntity<>(result ? "Success" : "Failed!", result ? HttpStatus.OK :HttpStatus.UNAUTHORIZED);
    }
}
